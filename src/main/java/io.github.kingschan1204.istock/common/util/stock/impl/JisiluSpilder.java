package io.github.kingschan1204.istock.common.util.stock.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.github.kingschan1204.istock.common.util.stock.StockSpider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * jisilu爬虫
 *
 * @author chenguoxiang
 * @create 2018-07-03 11:22
 **/
@Component("JisiluSpilder")
public class JisiluSpilder extends DefaultSpiderImpl {

    private static Logger log = LoggerFactory.getLogger(JisiluSpilder.class);

    /**
     * 得到ipo上市日期，最近公开报表，历史数据
     *
     * @param code
     * @return
     */
    public JSONObject crawHisPbPePriceAndReports(String code) throws Exception {
//        String useAgent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3346.9 Safari/537.36";
        String referrer = "https://www.jisilu.cn/data/stock/dividend_rate/#cn";
//        int timeout=8000;
        JSONObject result = new JSONObject();
        String url = String.format("https://www.jisilu.cn/data/stock/%s", code);
        log.info("craw jisilu page  :{}", url);
        StockSpider.enableSSLSocket();
        JSONArray jsons = new JSONArray();
        Document doc = null;
        try {
            doc = Jsoup.connect(url).userAgent(useAgent).referrer(referrer).timeout(timeout).get();
            // 得到ipo上市日期
            Elements spans = doc.getElementsByAttributeValue("style", "display: inline-block;width: 80px;color: #0088cb;font-size:12px;");
            spans.stream().forEach(span -> {
                if (span.text().matches("\\d{4}\\-\\d{2}\\-\\d{2}")) {
                    result.put("ipoDate", span.html());
                }
            });
            //拿定期公告
            Element table = doc.getElementById("tbl_periodicalreport");
            Elements tr = table.getElementsByTag("tr");
            JSONArray reportsJsons = new JSONArray();
            tr.stream().forEach(row -> {
                Elements a = row.getElementsByTag("a");
                String releaseDay = row.getElementsByTag("td").get(1).text();
                String link = a.get(0).attr("href");
                String title = a.get(0).text();
                JSONObject temp = new JSONObject();
                temp.put("releaseDay", releaseDay);
                temp.put("link", link);
                temp.put("title", title);
                reportsJsons.add(temp);
            });
            result.put("reports", reportsJsons);

            //解析历史数据
            Elements js = doc.getElementsByTag("script").eq(21);
            List<String> list = StockSpider.findStringByRegx(js.html(), "\\[.*\\]");
            //依次顺序 0:日期  1:PRICE  2:PB  3:PE
            String replaceRegex = "\'|\\[|\\]";
            String dates[] = list.get(0).replaceAll(replaceRegex, "").split(",");//日期
            String prices[] = list.get(1).replaceAll(replaceRegex, "").split(",");//价格
            String pbs[] = list.get(2).replaceAll(replaceRegex, "").split(",");//市净率
            String pes[] = list.get(3).replaceAll(replaceRegex, "").split(",");//市盈率
            JSONArray hisJson = new JSONArray();
            for (int i = 0; i < dates.length; i++) {
                JSONObject temp = new JSONObject();
                temp.put("date", dates[i]);
                temp.put("price", Double.parseDouble(prices[i]));
                temp.put("pb", Double.parseDouble(pbs[i]));
                temp.put("pe", Double.parseDouble(pes[i]));
                hisJson.add(temp);
            }
            result.put("hisdata", hisJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
