/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.engine.notification.pushservice;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.google.android.gcm.server.Sender;

/**
 * Adjustment for the new FCM Messagging endpoint 
 * @author raman
 *
 */
public class FCMSender extends Sender {

	public FCMSender(String key) {
		super(key);
	}

	public static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm";
	public static final String GCM_ENDPOINT = "https://gcm-http.googleapis.com/gcm";
	
	@Override
	protected HttpURLConnection post(String url, String contentType, String body) throws IOException {
		url = url.replace(GCM_ENDPOINT, FCM_ENDPOINT);
		return super.post(url, contentType, body);
	}
	

}
