package com.fliaping.trip.spider;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.net.SyslogAppender;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;

import java.util.List;
import java.util.Scanner;

/**
 * Created by Payne on 5/24/16.
 */
public class QunarPageProcessor implements PageProcessor{
    private Site site = Site
            .me()
            .setDomain("travel.qunar.com")
            .setSleepTime(30000)
            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");

    public Spider mSpider = null;

    public static final String URL_SEED = "http://travel.qunar.com/place/";
    public final String URL_PLACE = "^http://travel\\.qunar\\.com/p-cs\\d+-[a-z]+$";
    public final String URL_SIGHT_LIST = "^http://travel\\.qunar\\.com/p-cs\\d+-[a-z]+-jingdian$";
    public final String URL_SIGHT_LIST_PAGE = "^http://travel\\.qunar\\.com/p-cs\\d+-[a-z]+-jingdian-1-\\d{1,3}$";
    public final String URL_SIGHT = "^http://travel\\.qunar\\.com/p-oi\\d+-[a-z]+$";

    //在线测试可以,这里为什么不行 "^http://travel\\.qunar\\.com/p-cs\\d+-[a-z]+-jingdian(-1-\\d{1,3})?$"

    @Override
    public void process(Page page) {



        if (page.getHtml().regex("访问频率过高").match()){

            synchronized (Main.qunarSpider){
                long thisTime = System.currentTimeMillis();
                if(thisTime-Main.lastVerifyTime > 10000){
                    Main.qunarSpider.stop();
                    openUrlInBrower(page.getUrl().toString());
                    Scanner scanner = new Scanner(System.in);
                    scanner.next();

                    Main.lastVerifyTime = System.currentTimeMillis();
                    Main.qunarSpider.start();
                }
                page.addTargetRequest(page.getUrl().toString());
            }
        }
        //从种子页得到地区页
        if (page.getUrl().regex(URL_SEED).match()){
            List<String> place_links = page.getHtml().links().regex(URL_PLACE).all();
            page.addTargetRequests(place_links);
        }

        //地区页
        if(page.getUrl().regex(URL_PLACE).match()){

            System.out.println("地区:"+page.getHtml().xpath("/html/body/div[2]/div/div[2]/div/div[2]/div[1]/h1/text()").toString());

            //获得地区的景点列表
            List<String> sight_list_links = page.getHtml().links().regex(URL_SIGHT_LIST).all();
            page.addTargetRequests(sight_list_links);
            List<String> sight_list_links2 = page.getHtml().links().regex(URL_SIGHT_LIST_PAGE).all();
            page.addTargetRequests(sight_list_links2);
            page.setSkip(true); //地区页不要

            printList(sight_list_links);

            //景点列表页
        }else if (page.getUrl().regex(URL_SIGHT_LIST).match() || page.getUrl().regex(URL_SIGHT_LIST_PAGE).match()){
            System.out.println("景点列表:"+
                    page.getHtml().xpath("/html/body/div[2]/div/div[2]/div/span[1]/h1/text()").toString()+
                    " "+page.getUrl());

            //获得列表页中的景点页
            List<String> sight_links = page.getHtml().links().regex(URL_SIGHT).all();
            page.addTargetRequests(sight_links);
            //获取列表链接
            List<String> sight_list_links = page.getHtml().links().regex(URL_SIGHT_LIST).all();
            page.addTargetRequests(sight_list_links);
            List<String> sight_list_links2 = page.getHtml().links().regex(URL_SIGHT_LIST_PAGE).all();
            page.addTargetRequests(sight_list_links2);

            printList(sight_links);
            page.setSkip(true);  // 列表页也不要
            //景点页
        }else if (page.getUrl().regex(URL_SIGHT).match()){


            System.out.println("景点:"+page.getHtml().xpath("//*[@id=\"js_mainleft\"]/div[3]/h1/text()").toString());
            page.putField("sight_name", page.getHtml().xpath("//*[@id=\"js_mainleft\"]/div[3]/h1/text()").toString());
            if(page.getResultItems().get("sight_name") == null){
                page.setSkip(true);
                page.addTargetRequest(page.getUrl().toString());
            }
            Elements els = page.getHtml().getDocument().getElementsByClass("cur_score");
            if( els.size() > 0){
                page.putField("sight_score_qunar", els.get(0).text());
            }
            //page.putField("sight_intro", page.getHtml().getDocument().getElementsByClass("short").get(0).text());
            page.putField("sight_coordinate", page.getHtml().xpath("//*[@id=\"js_mainright\"]/div/div[2]/div/@latlng").toString());
            page.putField("sight_cover", page.getHtml().xpath("//*[@id=\"idNum\"]/li[0]/div[2]/img/@src)").toString());
            page.putField("html",page.getHtml().toString());

        }else {
            page.setSkip(true); //别的页面也不要
        }

    }

    @Override
    public Site getSite() {
        return site;
    }

    public void printList(List<String> list){
        for (int i=0;i<list.size();i++){
            System.out.println("list"+i+": "+list.get(i));
        }
    }
    public void openUrlInBrower(String url){
        try {
            java.net.URI uri = java.net.URI.create(url);

            // 获取当前系统桌面扩展

            java.awt.Desktop dp = java.awt.Desktop.getDesktop();

            // 判断系统桌面是否支持要执行的功能

            if (dp.isSupported(java.awt.Desktop.Action.BROWSE)) {
                //File file = new File("D:\\aa.txt");
                //dp.edit(file);// 　编辑文件

                dp.browse(uri);// 获取系统默认浏览器打开链接
                // dp.open(file);// 用默认方式打开文件
                // dp.print(file);// 用打印机打印文件
            }

        } catch (java.lang.NullPointerException e) {

            // 此为uri为空时抛出异常

            e.printStackTrace();

        } catch (java.io.IOException e) {

            // 此为无法获取系统默认浏览器

            e.printStackTrace();

        }
    }

}
