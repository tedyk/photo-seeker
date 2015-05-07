package org.tedka.photoseeker.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {

	/**
	 * Check if network is available. This is used before firing a search.
	 * @param context
	 * @return
	 */
	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}

	/**
	 * Fire the API URL on to the network, and receive server response.
	 *
	 * @param strURL
	 * @return
	 */
	public static String getServerResponse(String strURL) {
		StringBuffer response = new StringBuffer();
		HttpURLConnection urlConnection = null;
		String line = "";

		try {
			URL url = new URL(strURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			BufferedReader rd = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			while ((line = rd.readLine()) != null) {
				response.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			urlConnection.disconnect();
		}
		return response.toString();
	}
}
