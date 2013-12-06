/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.core.view;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.AccountDescriptor;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterManager;

public class BootActivity extends Activity {

    private Class<?> mLastStartedClass;

    App getApp() {
        return (App) getApplication();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.initialize(getApplicationContext(),
                    ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        setTheme(AppSettings.get().getCurrentThemeStyle());
        // LocalBroadcastManager.getInstance(this).registerReceiver(mOAuthLoginStateChangeReceiver,
        // new IntentFilter("" + SystemEvent.OAuthLoginStateChange));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        jumpToNext();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        jumpToNext();

    }

    @Override
    protected void onResume() {
        super.onResume();
        jumpToNext();
    }

    /*
     *
	 */
    void jumpToNext() {

        int accountCount = getApp().getAccountCount();
        if (accountCount == 0) {
            Intent intent = new Intent(getApplicationContext(), NewAccountActivity.class);
            overridePendingTransition(0, 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        } else {
            if (TwitterManager.get().hasValidTwitterInstance()) {

                Uri uriData = getIntent().getData();
                if (uriData != null) {
                    String host = uriData.getHost();
                    boolean urlValid = false;
                    finish();

                    if (host.contains("twitter")) {
                        if (getApp().getCurrentAccount().getSocialNetType() != SocialNetConstant.Type.Twitter) {
                            changeToFirstAccountOfType(SocialNetConstant.Type.Twitter);
                        }

                        if (uriData.getPath().contains("/status/")) {
                            String statusId = getUriPartAfterText(uriData, "status");
                            startTweetSpotlight(statusId);
                            urlValid = true;
                        } else if (uriData.getPath().contains("/intent/tweet")) {
                            String statusText = uriData.getQueryParameter("text");
                            if (uriData.getQueryParameterNames().contains("url")) {
                                statusText = statusText + " " + uriData.getQueryParameter("url");
                            }
                            if (uriData.getQueryParameterNames().contains("hashtags")) {
                                String[] hashtags = uriData.getQueryParameter("hashtags").split(",");
                                for (String hashtag : hashtags) {
                                    statusText = statusText + " #" + hashtag;
                                }
                            }
                            startHomeActivity(statusText);
                            urlValid = true;
                        }
                    } else if (host.contains("app.net")) {
                        if (getApp().getCurrentAccount().getSocialNetType() != SocialNetConstant.Type.Appdotnet) {
                            changeToFirstAccountOfType(SocialNetConstant.Type.Appdotnet);
                        }

                        if (uriData.getPath().contains("/post/")) {
                            String statusId = getUriPartAfterText(uriData, "post");
                            startTweetSpotlight(statusId);
                            urlValid = true;
                        }
                    }

                    if (!urlValid) {
                        Toast.makeText(getApplicationContext(), getString(R.string.unknown_intent),
                                Constant.DEFAULT_TOAST_DISPLAY_TIME).show();

                        startHomeActivity("");
                    }
                } else if (mLastStartedClass != HomeActivity.class) {
                    mLastStartedClass = HomeActivity.class;
                    // We don't want to come back here, so remove from the
                    // activity stack
                    finish();


                    Class<?> nextClass = HomeActivity.class;
                    if (!getApp().getTutorialCompleted()) {
                        nextClass = TutorialActivity.class;
                    }
                    Intent intent = new Intent(getApplicationContext(),
                            nextClass);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                    overridePendingTransition(0, 0);
                    startActivity(intent);
                }
            } else {
                // TODO: Handle this case
            }
        }

    }

    private String getUriPartAfterText(Uri uriData, String partBefore) {
        boolean nextPartStatus = false;
        for (String uriPart : uriData.getPathSegments()) {
            if (nextPartStatus == true) {
                return uriPart;
            }
            if (uriPart.toLowerCase().equals(partBefore)) {
                nextPartStatus = true;
            }
        }

        return "";
    }

    private void changeToFirstAccountOfType(SocialNetConstant.Type socialNetType) {
        for (AccountDescriptor account : getApp().getAccounts()) {
            if (account.getSocialNetType() == socialNetType) {
                getApp().setCurrentAccount(account.getId());
                return;
            }
        }
    }

    private void startTweetSpotlight(String statusId) {
        Intent tweetSpotlightIntent = new Intent(this, TweetSpotlightActivity.class);
        tweetSpotlightIntent.putExtra("statusId", statusId);
        tweetSpotlightIntent.putExtra("clearCompose", "true");
        overridePendingTransition(0, 0);
        startActivity(tweetSpotlightIntent);
    }

    private void startHomeActivity(String composeText) {
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setAction(Intent.ACTION_SEND);
        homeIntent.setType("text/plain");
        if (!composeText.isEmpty()) {
            homeIntent.putExtra(Intent.EXTRA_TEXT, composeText);
        }
        overridePendingTransition(0, 0);
        startActivity(homeIntent);
    }

    private void startProfileSpotlight(String userName) {
        Intent profileIntent = new Intent(this, ProfileActivity.class);
        profileIntent.putExtra("userScreenName", userName);
        profileIntent.putExtra("clearCompose", "true");
        overridePendingTransition(0, 0);
        startActivity(profileIntent);
    }
}
