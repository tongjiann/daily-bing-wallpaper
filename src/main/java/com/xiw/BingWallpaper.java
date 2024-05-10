package com.xiw;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.xiw.bean.Image;
import com.xiw.bean.Response;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BingWallpaper {

    private static final List<String> AREA_LIST = Arrays.asList("ar", "at", "au", "be", "br", "ca", "ch", "cl", "cn",
            "de", "dk", "es", "fi", "fr", "hk", "ie", "in", "it", "jp", "kr", "nl", "no", "nz", "ph", "pt", "ru", "se",
            "sg", "tw", "uk");

    public static void main(String[] args) {
        List<Image> allImages = getAllImages();
        downloadImages(allImages);
    }

    private static void downloadImages(List<Image> allImages) {
        allImages.stream()
                .filter(BingWallpaper::filterImage)
                .collect(Collectors.toMap(Image::getUrlName, Function.identity(), (x, y) -> x))
                .values()
                .forEach(image -> {
                    File file = getFile(image);
                    String url = getUHDUrl(image);
                    System.out.println("download:" + file.getName());
                    doDownload(file, url);
                });
    }

    private static boolean filterImage(Image image) {
        boolean isZhMkt = "zh-CN".equals(image.getMkt());
        boolean needDownload = !image.isWp() || isZhMkt;
        if (!needDownload) {
            return false;
        }
        String subDir;
        if (isZhMkt) {
            subDir = "normal";
        } else {
            subDir = "special";
        }
        boolean isExist = FileUtil.exist("./resources/uhd/" + subDir + File.separator, ".*?" + image.getUrlName() + ".*?");
        return !isExist;
    }

    private static void doDownload(File file, String url) {
        if (!file.exists()) {
            FileUtil.touch(file.getAbsolutePath());
        }
        HttpUtil.download(url, FileUtil.getOutputStream(file), true);
    }

    private static String getUHDUrl(Image image) {
        String urlPrefix = "https://www.bing.com/";
        String urlbase = image.getUrlbase();
        return CharSequenceUtil.format("{}{}_UHD.jpg", urlPrefix, urlbase);
    }

    private static File getFile(Image image) {
        String endDate = image.getEnddate();
        String year = endDate.substring(0, 4);
        String fileName = CharSequenceUtil.format("{}_{}.jpg", endDate, image.getUrlbase()
                .split("th\\?id=OHR.")[1]);
        String dirPath = "./resources/uhd/";
        if ("zh-cn".equals(image.getMkt().toLowerCase(Locale.getDefault()))) {
            dirPath = dirPath + "normal" + File.separator + year + File.separator;
        } else {
            dirPath = dirPath + "special" + File.separator + year + File.separator;
        }
        return new File(dirPath + fileName);
    }

    private static void setExtraInfo(Response response) {
        String mkt = response.getMarket().getMkt();
        response.getImages().forEach(e -> {
            e.setMkt(mkt);
            e.setUrlName(e.getUrlbase().split("th\\?id=OHR.")[1].split("_")[0]);
        });
    }

    private static List<String> buildUrlList() {
        List<String> urlList = new ArrayList<>();
        AREA_LIST.forEach(area -> {
            urlList.add("https://www.bing.com/HPImageArchive.aspx?format=js&pid=hp&og=1&idx=0&n=8&&mbl=1&cc=" + area);
            // urlList.add("https://www.bing.com/HPImageArchive.aspx?format=js&pid=hp&og=1&idx=7&n=8&&mbl=1&cc=" + area);
            // urlList.add("https://www.bing.com/HPImageArchive.aspx?format=js&pid=hp&og=1&idx=7&n=8&&mbl=1&cc=" + area);
        });
        return urlList;
    }

    private static List<Image> getAllImages() {
        List<String> urlList = buildUrlList();
        return urlList.stream().map(url -> {
            String result = HttpUtil.get(url);
            Response response = JSONUtil.toBean(result, Response.class);
            setExtraInfo(response);
            saveJson(result, response.getMarket().getMkt());
            return response.getImages().get(0);
        }).collect(Collectors.toList());
    }

    private static void saveJson(String result, String mkt) {
        LocalDate now = LocalDate.now();
        String dirPath = "./resources/json/" + now.toString().substring(0, 4) + File.separator + now + File.separator;
        File file = new File(dirPath + LocalDateTime.now() + "_" + mkt + ".json");
        FileUtil.writeString(result, file, Charset.defaultCharset());
    }

}

