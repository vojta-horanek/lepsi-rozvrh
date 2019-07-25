package cz.vitskalicky.lepsirozvrh.schoolsDatabase;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.ObjectLockingIndexedCollection;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class SchoolsDatabaseAPI {
    public static final String TAG = SchoolsDatabaseAPI.class.getSimpleName();

    public static final String SCHOOLS_DATABASE_URL = "https://sluzby.bakalari.cz/api/v1/municipality/"; //includes the last /


    /**
     * Fetches list ao all places (cities/towns/anything listed on their api) and returns their names
     * using the listener. If action fails, null is returned.
     * @param listListener listener using which data is returned. If action fails, null is returned.
     */
    public static void getCities(RequestQueue requestQueue, Context context, ListListener<String> listListener) {
        StringRequest request = new StringRequest(Request.Method.GET, SCHOOLS_DATABASE_URL, response -> {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(new ByteArrayInputStream(response.getBytes()));

                Element root = document.getDocumentElement();
                root.normalize();

                List<String> list = new LinkedList<>();

                NodeList nodeList = root.getElementsByTagName("name");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    list.add(nodeList.item(i).getTextContent());
                }

                listListener.method(list);
                return;
                /*for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                    Node item = root.getChildNodes().item(i);
                    item.get
                }*/

            } catch (ParserConfigurationException | IOException | SAXException e) {
                e.printStackTrace();
                listListener.method(null);
                return;
            }
        }, error -> {
            listListener.method(null);
            return;
        });
        requestQueue.add(request);
    }

    /**
     * Fetches list of all schools from Bakaláři api by fetching list of all cities and then list of
     * schools for each one. Might take some time. Returns null if fetching fails.
     * @param requestQueue Volley request queue to be used.
     * @param collectionListener listener using which data is returned. Returns null if fetching fails.
     */
    public static void getAllSchools(RequestQueue requestQueue, Context context, IndexedCollectionListener<SchoolInfo> collectionListener){
        SchoolsFetcher fetcher = new SchoolsFetcher();
        fetcher.getAllSchools(requestQueue, context, collectionListener);
    }

    /**
     * Helper class - I dah troubles with lambda expressions requiring final field - this solved it.
     * Read as: It works, do not touch it!
     */
    private static class SchoolsFetcher{
        int pocet = 0;
        int hotovo = 0;
        IndexedCollection<SchoolInfo> collection;
        IndexedCollectionListener<SchoolInfo> listener;

        public boolean getAllSchools(RequestQueue requestQueue, Context context, IndexedCollectionListener<SchoolInfo> collectionListener) {
            if (pocet != hotovo) {
                return false;
            }
            collection = new ObjectLockingIndexedCollection<>();
            collection.addIndex(SuffixTreeIndex.onAttribute(SchoolInfo.STRIPED_NAME));
            listener = collectionListener;

            getCities(requestQueue, context, list -> {
                if (list == null) {
                    Log.e(TAG, "Fetching cities list failed");
                    listener.method(null);
                    return;
                }
                pocet = list.size();
                hotovo = 0;
                for (String item : list) {
                    try {
                        //the API is weird - "." cause problems, you must stop before it
                        String[] split = item.split("\\.");
                        /*if (split.length > 0)*/
                        item = split[0];
                        String wrongurl = SCHOOLS_DATABASE_URL + URLEncoder.encode(item, StandardCharsets.UTF_8.toString());
                        String url = wrongurl.replace("+", "%20");

                        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
                            try {
                                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                DocumentBuilder db = dbf.newDocumentBuilder();
                                Document document = db.parse(new ByteArrayInputStream(response.getBytes()));

                                Element root = document.getDocumentElement();
                                root.normalize();

                                NodeList nodeList = root.getElementsByTagName("schoolInfo");
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    Node nodeItem = nodeList.item(i);
                                    SchoolInfo schoolInfo = new SchoolInfo();

                                    NodeList childNodes = nodeItem.getChildNodes();
                                    for (int j = 0; j < childNodes.getLength(); j++) {
                                        Node nodeItem2 = childNodes.item(j);
                                        switch (nodeItem2.getNodeName()) {
                                            case "id":
                                                schoolInfo.id = nodeItem2.getTextContent();
                                                break;
                                            case "name":
                                                schoolInfo.name = nodeItem2.getTextContent();
                                                schoolInfo.createStripedName(schoolInfo.name);
                                                break;
                                            case "schoolUrl":
                                                schoolInfo.url = nodeItem2.getTextContent();
                                                break;
                                        }
                                    }
                                    collection.add(schoolInfo);
                                }
                                requestFinished();

                            } catch (ParserConfigurationException | IOException | SAXException e) {
                                Log.e(TAG, "Parsing city school list failed: url: " + url + "response:\n" + response + "\n\n----------\nstack trace");
                                e.printStackTrace();
                                requestFinished();
                            }
                        }, error -> {
                            Log.d(TAG, "Fetching city school list failed: url:" + url + " error message: " + error.getMessage());
                            requestFinished();
                        });
                        requestQueue.add(request);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported URLEncoder encoding: " + StandardCharsets.UTF_8.toString() + "\nstack trace:");
                        e.printStackTrace();
                        requestFinished();
                    }
                }

                //collectionListener.method(collection);
            });
            return true;
        }

        private void requestFinished() {
            hotovo++;
            if (hotovo == pocet) {
                listener.method(collection);
            }
        }
    }

    public static interface ListListener<T> {
        public void method(List<T> list);
    }

    public static interface IndexedCollectionListener<T> {
        public void method(IndexedCollection<T> collection);
    }
}