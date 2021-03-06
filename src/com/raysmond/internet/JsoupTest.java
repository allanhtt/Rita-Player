package com.raysmond.internet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.raysmond.lyric.LRCSearchResult;
import com.raysmond.song.DownloadSong;


/**
 * 使用Jsoup这个开源的网页HTML解析器对百度音乐的搜索结果进行解析
 * 功能    从百度音乐搜索一个首歌的歌词，并返回搜索结果
 *      从百度音乐搜索一首歌，并返回搜索结果（解析出标题、歌手、专辑名和mp3下载链接）
 * @author Jiankun Lei
 *
 */
public class JsoupTest {

	public static void main(String[] args) 
	{
		getSongSearchResultFromBaidu("单车","陈奕迅",5);
	}
	
	/**
	 * 从一个URL下载歌词
	 * @param urlStr 
	 * @return
	 */
	public static String downloadLRC(String urlStr){
		String lyric = null;
		URL url = null;
		URLConnection conn = null;
		String nextLine = null;
		try{
			url = new URL(urlStr);
			conn = url.openConnection();
			conn.connect();
			BufferedReader reader = new BufferedReader(new 
					InputStreamReader(conn.getInputStream(),"utf-8"));
			StringBuffer buffer = new StringBuffer();
			while((nextLine = reader.readLine()) != null ){
				buffer.append(nextLine + "\n");
			}
			lyric = new String(buffer.toString());
			System.out.println(lyric);
		}catch(Exception e){
			
		}
		return lyric;
	}
	
	/**
	 * 从百度音乐中搜索歌词，并爬取搜索结果。分析网页获取下载链接，最后下载。
	 * @param title
	 * @param artist
	 * @param number
	 * @return
	 */
	public static ArrayList<LRCSearchResult> getSearchResultFromBaidu(String title,String artist,int number){
		ArrayList<LRCSearchResult> results = new ArrayList<LRCSearchResult>();
		try {
			System.out.println("searching: " + title);
			Document doc = Jsoup.connect("http://music.baidu.com/search/lrc?key=" 
					+ URLEncoder.encode(title,"UTF-8") ).get();
			Element s = doc.getElementById("lrc_list");
			Elements songList1 = s.getElementsByTag("ul").first().getElementsByTag("li");
			
			int counter = 0;
			for(Element e: songList1){
				Element songInfo = e.getElementsByClass("song-content").get(0);
					Element titleElement = e.getElementsByClass("song-title").get(0).getElementsByTag("a").first();
					if(titleElement==null) continue;
					String titleStr = titleElement.attr("title");
					if(number==1){ //得到第一个精确一点的歌词
						if(!titleStr.equalsIgnoreCase(title)) continue;
					}
					Element artistElement = e.getElementsByClass("artist-title").get(0).getElementsByTag("a").first();
					
					String artistStr = artistElement.ownText();
					System.out.println("title:" + titleStr);
					System.out.println("artist:" + artistStr);
					
				Element download = e.getElementsMatchingOwnText("下载LRC歌词").first();
					String linkData = download.className();
					String linkDownload = linkData.substring(linkData.indexOf("data2"),  linkData.indexOf(".lrc") + 4 );
					System.out.println("download link:"+linkDownload);
					
					LRCSearchResult item = new LRCSearchResult();
					item.setSongTitle(titleStr);
					item.setSongArtist(artistStr);
					item.setDownloadUrl("http://music.baidu.com/" + linkDownload);
					item.setLrcText(downloadLRC("http://music.baidu.com/" + linkDownload));
					
					results.add(item);
					counter++;
					if(counter>=number)return results;
			}
		} catch (IOException e) { 
			//e.printStackTrace();
			System.err.println("Network error. Search lyrics failed.");
		}
		return results;
	}

	/**
	 * 从百度音乐搜索一首歌，解析搜索页面HTML，得到结果
	 * @param title   歌名
	 * @param artist  歌手
	 * @param number  指定需要搜索的结果数
	 * @return
	 */
	public static ArrayList<DownloadSong> getSongSearchResultFromBaidu(String title,String artist,int number){
		ArrayList<DownloadSong> results = new ArrayList<DownloadSong>();
		try {
			System.out.println("searching: " + title);
			Document doc = Jsoup.connect("http://music.baidu.com/search/song?key=" + URLEncoder.encode(title,"UTF-8") ).get();
			Element s = doc.getElementById("result_container").getElementsByClass("song-list").first();
			Elements songList1 = s.getElementsByTag("ul").first().getElementsByTag("li");
			int counter = 0;
			for(Element e: songList1){
				Element songInfo = e.getElementsByClass("song-item").first();
					Element titleElement = e.getElementsByClass("song-title").first().getElementsByTag("a").first();
					if(titleElement==null) continue;
					String titleStr = titleElement.attr("title");
					if(number==1){ //得到第一个精确一点的歌词
						if(!titleStr.equalsIgnoreCase(title)) continue;
					}
					Element artistElement = e.getElementsByClass("singer").first().getElementsByTag("a").first();
					Element albumElement = e.getElementsByClass("album-title").first().getElementsByTag("a").first();
					
					String artistStr = null;
					if(artistElement!=null) artistStr = artistElement.attr("title");
					String albumStr = null;
					if(albumElement!=null) albumStr = albumElement.attr("title");
					
					String songId = e.attr("data-songitem");
					int startIndex = songId.indexOf("\"sid\":") + 6;
					int endIndex = songId.indexOf("}");
					songId = songId.substring(startIndex, endIndex);
					String downloadUrlPage = "http://music.baidu.com/song/" + songId + "/download";
					
					Document downloadPage = Jsoup.connect(downloadUrlPage).get();
					String downData = null;
					Element downloadBut = downloadPage.getElementById("download");
					if(downloadBut==null)continue;
					downData = downloadBut.attr("href");
					downData = "http://music.baidu.com" + downData;
					/*
					Elements downloads = downloadPage.getElementById("form").getElementsByTag("ul").first().getElementsByTag("li");
					
					if(downloads.size()>=2){
						downData = downloads.get(1).attr("data-data");
						System.out.println("downData:" + downloads.get(1).toString());
					}
					else if(downloads.size()==1){
						downData = downloads.first().attr("data-data");
					}
					*/
					
					System.out.println("downData:" + downData);
					
					String rate = null;
					String link = null;
					if(downData!=null){
						//rate = downData.substring(downData.indexOf("\"rate\":") + 7, downData.indexOf(","));
						//link = downData.substring(downData.indexOf("\"link\":\"") + 8, downData.indexOf("\"}"));
					}
					else continue;
					
					System.out.println("title:" + titleStr);
					System.out.println("artist:" + artistStr);
					System.out.println("album:" + albumStr);
					System.out.println("download:" + downloadUrlPage);
					System.out.println("link:" + link);
					System.out.println("rate:" + rate);
					
					DownloadSong song = new DownloadSong(titleStr,artistStr,
							(artistStr + " - " + titleStr + ".mp3"),
							0,downData,albumStr);
					results.add(song);
					counter++;
					if(counter>=number) return results;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return results;
	}
	
}
