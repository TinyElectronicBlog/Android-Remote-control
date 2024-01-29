package com.tinyelectronicblog.remotecontrol.activity.remotecontrol;

/**
 * MIT License
 * Copyright (c) 2024 TinyElectronicBlog
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.inputmethod.InputMethodManager;
import com.tinyelectronicblog.remotecontrol.device.Device;
import com.tinyelectronicblog.remotecontrol.tinyelectronicblogpackage.debug.Debug;
import com.tinyelectronicblog.remotecontrol.tinyelectronicblogpackage.multithreading.ForegroundServiceTask;
import com.tinyelectronicblog.remotecontrol.activity.loading.LoadingActivity;
import com.tinyelectronicblog.remotecontrol.activity.remotecontrol.dashboard.DashboardAlgorithm;
import com.tinyelectronicblog.remotecontrol.activity.remotecontrol.on_off.OnOffAlgorithm;
import com.tinyelectronicblog.remotecontrol.activity.remotecontrol.settings.SettingsFragmentAlgorithm;
import com.tinyelectronicblog.remotecontrol.tinyelectronicblogpackage.multithreading.MainThreadTask;
import com.tinyelectronicblog.remotecontrol.tinyelectronicblogpackage.multithreading.NewThreadTask;

//Algorithm of the service that manages the remote control
public class RemoteControlAlgorithm extends ForegroundServiceTask {

    //number of commands in sending
    public static int numberOfCommandsInSending = 0;

    private static RemoteControlAlgorithm reference = null;

    private RemoteControlActivity activity;

    private boolean canSendCommands, serviceIsRunning, stopRequest;
    private int state;

    private RemoteControlAlgorithm(String name) {
        super(name);
        canSendCommands = false;
        state = 0;
        stopRequest = false;
        serviceIsRunning = false;
    }

    static void onResume(RemoteControlActivity activity) {
        if (reference == null) {
            reference = new RemoteControlAlgorithm("RemoteControlAlgorithm");
            reference.start(activity);
        }

        reference.activity = activity;
    }

    static void onPause() {
    }

    @Override
    public void toDo() {
        Debug.print("RemoteControlAlgorithm start");

        serviceIsRunning = true; //RemoteControlService started
        canSendCommands = false;
        state = 0;
        stopRequest = false;
        animationTask.start();

        while (true) {
            try {// this while must not be interrupted due to exceptions

                //STEP 0: WAITING FOR GOOGLE DRIVE TO BE READY
                animationChar = "0";
                if (!SettingsFragmentAlgorithm.isDriveReady()) {
                    state = 0;
                    while (!stopRequest) {
                        try {
                            if (SettingsFragmentAlgorithm.isDriveReady()) break;
                            Thread.sleep(5);
                        } catch (Exception e) {
                            Debug.print(e.toString());
                        }
                    }
                }

                long time = System.currentTimeMillis();

                if (stopRequest) break;

                //STEP 1: READING FILES ON GOOGLE DRIVE
                animationChar = "1";
                long time1 = 0;
                if (Debug.isEnabled()) time1 = Debug.millis();
                canSendCommands = false; //pause sending commands
                try {
                    if (Device.updateDeviceFiles() != 0)
                        state = -1; //reads online files related to device and, for each one, obtains buttons and values
                    else {
                        if (Debug.isEnabled()) {
                            long time2 = System.currentTimeMillis();
                            Debug.print("Execution time step 1-1: " + (time2 - time1) + " ms");
                            time1 = time2;
                        }
                        if (Device.updateAckCommandFiles() != 0)
                            state = -2; //Reads online command and ack files, deleting those no longer needed. Adds, removes, activates and/or deactivates buttons in OnOffFragment
                        else {
                            if (Debug.isEnabled()) {
                                long time2 = System.currentTimeMillis();
                                Debug.print("Execution time step 1-2: " + (time2 - time1) + " ms");
                            }
                            state = 1;
                        }
                    }
                } catch (Exception e) {
                    state = -3;
                }

                if (stopRequest) break;

                //STEP 2: DASHBOARD AND ON-OFF UI UPDATE
                animationChar = "2";
                //NOTE: if the update of the ui fails is not a problem, it means that the related fragments are not displayed
                if (state == 1) {//if step 1 ended without errors
                    canSendCommands = true; //restart sending commands
                    time1 = System.currentTimeMillis();

                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm.isAcceptingText())
                        Debug.print("On-Off UI not updated"); //I skip the update if I am entering a command with a custom value, otherwise the keyboard would close cyclically
                    else try {
                        OnOffAlgorithm.updateUI();
                    } catch (Exception e) {
                        Debug.print(e.toString());
                    }

                    if (Debug.isEnabled()) {
                        long time2 = System.currentTimeMillis();
                        Debug.print("On Off UI creation time " + (time2 - time1) + " ms");
                        time1 = time2;
                    }

                    try {
                        DashboardAlgorithm.updateUI();
                    } catch (Exception e) {
                        Debug.print(e.toString());
                    }

                    if (Debug.isEnabled()) {
                        long time2 = System.currentTimeMillis();
                        Debug.print("Dashboard UI creation time " + (time2 - time1) + " ms");
                    }

                } else { //if step 1 ended with errors

                    Debug.print("STEP 2 error, state = " + state);
                    try {
                        OnOffAlgorithm.updateUI();
                    } catch (Exception e) {
                        Debug.print(e.toString());
                    }

                    try {
                        DashboardAlgorithm.updateUI();
                    } catch (Exception e) {
                        Debug.print(e.toString());
                    }

                }

                if (stopRequest) break;

                //STEP 3: WAITING
                animationChar = "3";
                try {
                    if (state == 1) { //if step 1 ended without errors
                        final long wait = Integer.parseInt(LoadingActivity.getSettings().dataUpdate) * 1000L - (System.currentTimeMillis() - time);
                        if (wait > 0 && !stopRequest)
                            Thread.sleep(wait); //In the meantime you can send commands
                        canSendCommands = false; //Sending commands paused, new requests will be processed at the next cycle
                        while (numberOfCommandsInSending > 0 && !stopRequest) {//if necessary, wait for the end of sending commands
                            Thread.sleep(5);
                        }
                    } else {
                        if (!stopRequest) Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Debug.print(e.toString());
                }
            } catch (Exception e) {
                Debug.print(e.toString());
            }
        }

        serviceIsRunning = false;
        canSendCommands = false;
        stopRequest = false;
        state = 0;
        animationChar = ".";
    }

    public static boolean isServiceRunning() {
        return reference.serviceIsRunning;
    }

    public static boolean canSendCommands() {
        return reference.canSendCommands;
    }

    public static int getAlgorithmState() {
        return reference.state;
    }

    public static void setTextViewText (String text) {
        try {reference.activity.activityTextView.setText(text);}
        catch (Exception e) {Debug.print(e.toString());}
    }

    public static void destroy(int typeOfOutput) {
        RemoteControlAlgorithm.stopService();
        if (typeOfOutput == 0)
            if (!LoadingActivity.getSettings().defaultSettings(reference.activity, false))
                Debug.print("Warning, settings not saved on file during logout"); //I set the default values of the settings (except the language) and save them on file
        Device.resetDeviceArray();
        OnOffAlgorithm.destroy();
        DashboardAlgorithm.destroy();
        SettingsFragmentAlgorithm.destroy();
        RemoteControlActivity activity = reference.activity;
        Intent resultIntent = new Intent();
        if (typeOfOutput == 0) resultIntent.putExtra("type_of_output", "disconnection");
        else if (typeOfOutput == 1) resultIntent.putExtra("type_of_output", "application_exit");
        activity.setResult(Activity.RESULT_OK, resultIntent);
        reference.activity = null;
        reference = null;
        activity.finish();
    }

    public static boolean getStopRequest() {
        return reference.stopRequest;
    }

    //to be performed only with the visible RemoteControlActivity
    private static void stopService() {
        reference.stopRequest = true;
        reference.stopForegroundService(reference.activity);
    }

    private int animationStep = 0;
    private String algorithmAnimation(String s) {
        String space1 = "         ";
        String space2 = "               ";
        String space3 = "  ";
        final String animation0 = s+"\n"+s+space1+s+"\n"+s+space2+s+"\n"+s+space1+s+"\n"+s+"\n";
        final String animation1 =   "\n"+s+space1+s+"\n"+s+space2+s+"\n"+s+space1+s+"\n"+s+"\n";
        final String animation2 = s+"\n"+s+space3+space1+"\n"    +s+space2+s+"\n"+s+space1+s+"\n"+s+"\n";
        final String animation3 = s+"\n"+s+space1+s+"\n"+s+space3+space2+"\n"    +s+space1+s+"\n"+s+"\n";
        final String animation4 = s+"\n"+s+space1+s+"\n"+s+space2+s+"\n"+s+space3+space1+"\n"    +s+"\n";
        final String animation5 = s+"\n"+s+space1+s+"\n"+s+space2+s+"\n"+s+space1+s+"\n\n";
        final String animation6 = s+"\n"+s+space1+s+"\n"+s+space2+s+"\n"+space1+space3+s+"\n"+s+"\n";
        final String animation7 = s+"\n"+s+space1+s+"\n"+space2+space3+s+"\n"+s+space1+s+"\n"+s+"\n";
        final String animation8 = s+"\n"+space1+space3+s+"\n"+s+space2+s+"\n"+s+space1+s+"\n"+s+"\n";
        String r;
        switch (animationStep) {
            case 0:
                r = animation0;
                break;
            case 1:
                r = animation1;
                break;
            case 2:
                r = animation2;
                break;
            case 3:
                r = animation3;
                break;
            case 4:
                r = animation4;
                break;
            case 5:
                r = animation5;
                break;
            case 6:
                r = animation6;
                break;
            case 7:
                r = animation7;
                break;
            case 8:
                r = animation8;
                break;
            default:
                r = "";
                break;
        }
        animationStep++;
        if (animationStep > 8) animationStep = 0;
        return r;
    }
    private String animationChar = ".";
    NewThreadTask animationTask = new NewThreadTask() {
        @Override
        public void toDo() {
            while (serviceIsRunning) {
                try {
                    final String t = algorithmAnimation(animationChar);
                    new MainThreadTask() {
                        @Override
                        public void toDo() {
                            try {
                                activity.animationTextView.setText(t);
                            } catch (Exception e) {
                                Debug.print(e.toString());
                            }
                        }
                    }.start();
                    Thread.sleep(300);
                } catch (Exception e) {
                    Debug.print(e.toString());
                }
            }
        }
    };

}