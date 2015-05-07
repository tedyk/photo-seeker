package org.tedka.photoseeker.channel.flickr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tedka.photoseeker.channel.base.ChannelController;
import org.tedka.photoseeker.channel.base.ChannelModel;
import org.tedka.photoseeker.util.Network;

import java.util.ArrayList;

/**
 * Controller to handle Flickr search
 */
public class FlickrController extends ChannelController {

	/** Flickr API KEY  */
	private static String API_KEY = "7f7b1aafb9ca948e1f9bd25de2db706c";

	/** DataStructure to hold search results from Flickr */
	private static ArrayList<ChannelModel> imageFeed = new ArrayList<>();

	/** Singleton instance of this controller **/
	private static FlickrController myInstance = null;

	/**
	 * Placeholder singleton constructor
	 */
	private FlickrController() {}

	/**
	 * Return the singleton instance of Flickr Controller
	 *
	 * @return
	 */
	public static FlickrController getInstance() {
		if(myInstance == null) {
			myInstance = new FlickrController();
		}
		return myInstance;
	}

	/**
	 * Return the search results data
	 *
	 * @return
	 */
	public ArrayList<ChannelModel> getImageFeed() {
		return imageFeed;
	}

	/**
	 * Get the photos response from server, and parse the JSON response and populate the
	 * DataStructure that is to hold the results
	 *
	 * @param searchTerm
	 * @param currentPage
	 * @return
	 */
	public static ArrayList<FlickrModel> getPhotos(String searchTerm, int currentPage) {

		ArrayList<FlickrModel> photos = new ArrayList<>();

		String serverResponse = Network.getServerResponse(getSearchUrl(
				searchTerm.replaceAll(" ", ""), currentPage));

		if (!serverResponse.isEmpty()) {
			try {
				JSONObject photosObj = new JSONObject(serverResponse).getJSONObject("photos");

				JSONArray jArray = photosObj.getJSONArray("photo");

				for (int i = 0; i < jArray.length(); i++) {

					JSONObject jObj = jArray.getJSONObject(i);

					FlickrModel photo = new FlickrModel();

					photo.setId(jObj.getString("id"));
					photo.setServer(jObj.getString("server"));
					photo.setSecret(jObj.getString("secret"));
					photo.setFarm(jObj.getString("farm"));
					photo.setTitle(jObj.getString("title"));

					photos.add(photo);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return photos;
	}

	/**
	 * Construct and return the URL for the full-size photo at a given position in the results
	 *
	 * @param photoPosition
	 * @return
	 */
	public String getPhotoUrl(int photoPosition) {
		FlickrModel photo = (FlickrModel)imageFeed.get(photoPosition);
		String url = String.format("https://farm%s.staticflickr.com/%s/%s_%s_m.jpg",
				photo.getFarmId(), photo.getServerId(), photo.getId(), photo.getSecretId());
		return url;
	}

	/**
	 * Construct and return the search url
	 *
	 * @param tag
	 * @param curPage
	 * @return
	 */
	private static String getSearchUrl(String tag, int curPage) {
		String urlFormat = "https://api.flickr.com/services/rest/?method=flickr.photos.search"+
				"&api_key=%s&tags=%s&per_page=20&page=%s&format=json&nojsoncallback=1";
		String url = String.format(urlFormat, API_KEY, tag, curPage);
		return url;
	}


}
