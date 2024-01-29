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

import static android.app.ProgressDialog.show;
import static com.tinyelectronicblog.remotecontrol.Strings.device_button_limit_sending_commands1;
import static com.tinyelectronicblog.remotecontrol.Strings.device_button_limit_sending_commands2;
import static com.tinyelectronicblog.remotecontrol.Strings.remotecontrol_text_view;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.tinyelectronicblog.remotecontrol.R;
import com.tinyelectronicblog.remotecontrol.activity.loading.LoadingActivity;
import com.tinyelectronicblog.remotecontrol.activity.remotecontrol.on_off.OnOffAlgorithm;
import com.tinyelectronicblog.remotecontrol.databinding.ActivityRemoteControlBinding;

public class RemoteControlActivity extends AppCompatActivity {

    TextView animationTextView;
    TextView activityTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityRemoteControlBinding binding = ActivityRemoteControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        animationTextView = binding.animationTextView;
        activityTextView = binding.activityTextView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_on_off, R.id.navigation_dashboard, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        activityTextView.setText(remotecontrol_text_view.get(LoadingActivity.getSettings().languageID));
    }

    @Override
    protected void onResume () {
        super.onResume();
        RemoteControlAlgorithm.onResume(this);
    }

    @Override
    protected void onPause () {
        super.onPause();
        RemoteControlAlgorithm.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        animationTextView = null;
        activityTextView = null;
    }

}