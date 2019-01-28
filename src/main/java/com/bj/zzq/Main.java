package com.bj.zzq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bj.zzq.utils.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

/**
 * @Author: zhaozhiqiang
 * @Date: 2019/1/11
 * @Description: 只在早上七点抢
 */
public class Main {
    private static Scheduler scheduler;
    private static Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) throws SchedulerException {
        //doBusiness();
    }

    private static void doBusiness() throws SchedulerException, ParseException {
        //集中火力
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    log.info("集中火力timer任务开始-------------------------------");
                    //focusOnFire();
                    log.info("集中火力timer任务结束-------------------------------");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        String needDate = "";
        String cronFocusOnFire = "";
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 6);
        instance.set(Calendar.MINUTE, 59);
        instance.set(Calendar.SECOND, 59);
        instance.set(Calendar.MILLISECOND, 500);
        Date now = new Date();

        if (now.after(instance.getTime())) {
            instance.add(Calendar.DAY_OF_YEAR, 1);
        }
        Date start = instance.getTime();
        timer.schedule(timerTask, start, 1000 * 60 * 60 * 24 * 1);

        //平时捡漏
        PickScheduler mainScheduler = new PickScheduler();
        mainScheduler.schedulerJob();
    }

    /**
     * @param orderDate 约车时间 yyyy-MM-dd
     * @return 开火时间
     */
    private static Date generateFireTimeByOrderDate(String orderDate) {
        // 77 17 27 37 47 57 67
        Date date = DateUtils.strToDate(orderDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date fireTime = calendar.getTime();
        Calendar now = Calendar.getInstance();
        if (fireTime.after(now.getTime())) {
            return null;
        }
        return fireTime;
    }

    private static Trigger getFireTrigger(UserInfo userInfo, String orderDate) {
        Date fireTime = generateFireTimeByOrderDate(orderDate);
        SimpleTrigger fireTrigger = null;
        if (fireTime != null) {
            fireTrigger = TriggerBuilder.newTrigger().withIdentity("fire_trigger_" + userInfo.getUsername(), "fire_group_order").withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(10).withIntervalInMilliseconds(1)).startNow().build();
        }
        return fireTrigger;
    }

    private static Trigger getPickTrigger(UserInfo userInfo, String orderDate) {
        Date pickEndTime = getPickEndTime(orderDate);
        Date startTime = generateFireTimeByOrderDate(orderDate);
        Trigger pickTrigger = TriggerBuilder.newTrigger().withIdentity("pick_trigger_" + userInfo.getUsername(), "pick_group_order").withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(10).withIntervalInMilliseconds(1)).startNow().build();

        if (startTime != null) {

        }

        if (fireTime != null) {
            fireTrigger = TriggerBuilder.newTrigger().withIdentity("trigger_" + userInfo.getUsername(), "group_order").withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(10).withIntervalInMilliseconds(1)).startNow().build();
        }
        return fireTrigger;
    }

    /**
     * 捡漏结束时间（默认前一天晚上10点结束）
     *
     * @param orderDate
     */
    private static Date getPickEndTime(String orderDate) {
        Date date = DateUtils.strToDate(orderDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void doJob(UserInfo userInfo, String orderDate) {


        JobDetail orderJob = getOrderJob(userInfo, orderDate);

    }

    private static JobDetail getOrderJob(UserInfo userInfo, String orderDate) {
        JobDetail jobDetail = JobBuilder.newJob(PickJob.class).withIdentity("job_" + userInfo.getUsername(), "job_group").storeDurably(true).build();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        jobDataMap.put("username", userInfo.getUsername());
        jobDataMap.put("password", userInfo.getPassword());
        jobDataMap.put("email", userInfo.getEmail());
        return jobDetail;
    }

    /**
     * 平时捡漏
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public static void picker(String orderDate) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException, InterruptedException {
        String xxzh = login();
        //获取用户信息
        String cnbh = ConfProperties.cnbh;
        if (StringUtils.isBlank(cnbh)) {
            //获取cnbh
            HashMap<String, String> params4 = new HashMap<>();
            params4.put("xxzh", xxzh);
            String resultStudent = HttpUtils.doHttp("get", HttpUtils.userInfoUrl, null, params4);
            JSONObject jsonObject = (JSONObject) JSON.parse(resultStudent);
            JSONObject data = jsonObject.getJSONObject("data");
            cnbh = data.getString("CNBH");
        }

        //查询是否有号
        HashMap<String, String> params3 = new HashMap<>();
        HttpUtils.addJsonpParams(params3);
        params3.put("xxzh", xxzh);
        String result3 = HttpUtils.doHttp("get", HttpUtils.orderQueryUrl, null, params3);
        //jQuery19103597663931350108_1547188429681({
        //  "data": null,
        //  "code": 111,
        //  "message": "访问太过频繁,请输入验证码！"
        //})
        if (result3.contains("验证码")) {
            Thread.sleep(5000);
            return;
        }
        String replace = result3.substring(result3.indexOf("\"") + 1, result3.length() - 2).replace("\\r\\n", "").replace("\\", "");
        JSONObject jsonObject = (JSONObject) JSON.parse(replace);
        JSONObject JSONObject2 = (JSONObject) jsonObject.get("data");
        JSONArray uiDatas = JSONObject2.getJSONArray("UIDatas");
        for (int i = 0; i < uiDatas.size(); i++) {
            JSONObject o = (JSONObject) uiDatas.get(i);
            Integer sl = o.getInteger("SL");
            String yyrq = o.getString("Yyrq"); //2019/01/15 17:58:51
            String Xnsd = o.getString("Xnsd"); // 时间段 1点到5点 简称 15
            yyrq = yyrq.substring(0, 10).replaceAll("/", "-");
            String xnsd = ConfProperties.timeSlot;
            if (StringUtils.isBlank(xnsd)) {
                xnsd = "15";
            }
            if (sl <= 0 || !xnsd.equals(Xnsd) || !orderDate.equals(yyrq)) {
                continue;
            }

            //有号，可以预约了
            HashMap params5 = new HashMap();
            params5.put("cnbh", cnbh);
            params5.put("xxzh", xxzh);
            params5.put("params", cnbh + "." + yyrq + "." + xnsd + ".");
            params5.put("isJcsdYyMode", "1");
            HttpUtils.addJsonpParams(params5);
            String result5 = HttpUtils.doHttp("get", HttpUtils.orderUrl, null, params5);
            result5 = result5.substring(result5.indexOf("\"") + 1, result5.length() - 2).replace("\\r\\n", "").replace("\\", "");
            JSONObject jsonObject5 = (JSONObject) JSON.parse(result5);
            int code = jsonObject5.getInteger("code");
            if (code == 0) {
                log.info("抢到了！！！！！！ " + orderDate + " " + xnsd);
                String finalXnsd = xnsd;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String howTime = "";
                        if (finalXnsd.equals("15")) {
                            howTime = "下午1点到5点";
                        } else if (finalXnsd.equals("812")) {
                            howTime = "上午8点到12点";
                        } else if (finalXnsd.equals("58")) {
                            howTime = "下午5点到8点";
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        int numInWeek = 0;
                        try {
                            Date date = sdf.parse(orderDate);
                            Calendar instance = Calendar.getInstance();
                            instance.setTime(date);
                            numInWeek = instance.get(Calendar.DAY_OF_WEEK);
                        } catch (ParseException e) {
                            log.info("解析日期失败", e);
                        }
                        String numInWeekUpper = numWeekToUpper(numInWeek);
                        EmailUtils.sendEmail("龙泉驾校约车成功", "恭喜你约到 " + orderDate + " (周" + numInWeekUpper + ") " + howTime + "的车，详情请登录学车不查看！");
                    }
                }).start();
                return;
            }
        }


    }

    public static String numWeekToUpper(int numInWeek) {
        String numInWeekUpper = "";
        switch (numInWeek) {
            case 1: {
                numInWeekUpper = "日";
                break;
            }
            case 2: {
                numInWeekUpper = "一";
                break;
            }
            case 3: {
                numInWeekUpper = "二";
                break;
            }
            case 4: {
                numInWeekUpper = "三";
                break;
            }
            case 5: {
                numInWeekUpper = "四";
                break;
            }
            case 6: {
                numInWeekUpper = "五";
                break;
            }
            case 7: {
                numInWeekUpper = "六";
                break;
            }
            default:
                numInWeekUpper = "转换出错，请输入正确的星期数字(1-7)";
        }
        return numInWeekUpper;
    }

//    public static void focusOnFire() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, URISyntaxException, InterruptedException {
//
//
//        //预约日期
//        String orderDate = ConfProperties.orderDate;
//        if (StringUtils.isBlank(orderDate)) {
//            //如果不配置预约日期，默认只预约周六周日的
//            Calendar instance = Calendar.getInstance();
//            int weekNum = instance.get(Calendar.DAY_OF_WEEK);//2-星期一   1-星期日，醉了
//            if (weekNum != 1 && weekNum != 2) {
//                return;
//            }
//            //预约下周的车
//            instance.add(Calendar.DAY_OF_YEAR, 6);
//            Date time = instance.getTime();
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//            orderDate = sdf.format(time);
//        }
//
//        String xxzh = login();
//        //获取用户信息
//        String cnbh = ConfProperties.cnbh;
//        if (StringUtils.isBlank(cnbh)) {
//            //获取cnbh
//            HashMap<String, String> params4 = new HashMap<>();
//            params4.put("xxzh", xxzh);
//            String resultStudent = HttpUtils.doHttp("get", HttpUtils.userInfoUrl, null, params4);
//            JSONObject jsonObject = (JSONObject) JSON.parse(resultStudent);
//            JSONObject data = jsonObject.getJSONObject("data");
//            cnbh = data.getString("CNBH");
//        }
//
//        boolean isOrder = false;
//        int count = 0;
//        //100次还抢不到那就真的抢不到了
//        while (!isOrder || count++ > 100) {
//            //查询是否有号
//            HashMap<String, String> params3 = new HashMap<>();
//            HttpUtils.addJsonpParams(params3);
//            params3.put("xxzh", xxzh);
//            String result3 = HttpUtils.doHttp("get", HttpUtils.orderQueryUrl, null, params3);
//            //jQuery19103597663931350108_1547188429681({
//            //  "data": null,
//            //  "code": 111,
//            //  "message": "访问太过频繁,请输入验证码！"
//            //})
//            if (result3.contains("验证码")) {
//                Thread.sleep(5000);
//                continue;
//            }
//            String replace = result3.substring(result3.indexOf("\"") + 1, result3.length() - 2).replace("\\r\\n", "").replace("\\", "");
//            JSONObject jsonObject = (JSONObject) JSON.parse(replace);
//            JSONObject JSONObject2 = (JSONObject) jsonObject.get("data");
//            JSONArray uiDatas = JSONObject2.getJSONArray("UIDatas");
//            for (int i = 0; i < uiDatas.size(); i++) {
//                JSONObject o = (JSONObject) uiDatas.get(i);
//                Integer sl = o.getInteger("SL");
//                String yyrq = o.getString("Yyrq"); //2019/01/15 17:58:51
//                String Xnsd = o.getString("Xnsd"); // 时间段 1点到5点 简称 15
//                yyrq = yyrq.substring(0, 10).replaceAll("/", "-");
//                String xnsd = ConfProperties.timeSlot;
//                if (StringUtils.isBlank(xnsd)) {
//                    xnsd = "15";
//                }
//                if (sl <= 0 || !xnsd.equals(Xnsd) || !orderDate.equals(yyrq)) {
//                    continue;
//                }
//
//                //有号，可以预约了
//                HashMap params5 = new HashMap();
//                params5.put("cnbh", cnbh);
//                params5.put("xxzh", xxzh);
//                params5.put("params", cnbh + "." + yyrq + "." + xnsd + ".");
//                params5.put("isJcsdYyMode", "1");
//                HttpUtils.addJsonpParams(params5);
//                String result5 = HttpUtils.doHttp("get", HttpUtils.orderUrl, null, params5);
//                result5 = result5.substring(result5.indexOf("\"") + 1, result5.length() - 2).replace("\\r\\n", "").replace("\\", "");
//                JSONObject jsonObject5 = (JSONObject) JSON.parse(result5);
//                int code = jsonObject5.getInteger("code");
//                if (code == 0) {
//                    log.info("抢到了！！！！！！ " + orderDate + " " + xnsd);
//                    String finalXnsd = xnsd;
//                    String finalOrderDate = orderDate;
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String howTime = "";
//                            if (finalXnsd.equals("15")) {
//                                howTime = "下午1点到5点";
//                            } else if (finalXnsd.equals("812")) {
//                                howTime = "上午8点到12点";
//                            } else if (finalXnsd.equals("58")) {
//                                howTime = "下午5点到8点";
//                            }
//                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//                            int numInWeek = 0;
//                            try {
//                                Date date = sdf.parse(finalOrderDate);
//                                Calendar instance = Calendar.getInstance();
//                                instance.setTime(date);
//                                numInWeek = instance.get(Calendar.DAY_OF_WEEK);
//                            } catch (ParseException e) {
//                                log.info("解析日期失败", e);
//                            }
//                            String numInWeekUpper = numWeekToUpper(numInWeek);
//                            EmailUtils.sendEmail("龙泉驾校约车成功", "恭喜你约到 " + finalOrderDate + " (周" + numInWeekUpper + ") " + howTime + "的车，详情请登录学车不查看！");
//                        }
//                    }).start();
//                    isOrder = true;
//                    break;
//                }
//            }
//        }
//    }

    public static String login() throws URISyntaxException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HashMap params = new HashMap();
        HttpUtils.addJsonpParams(params);
        params.put("username", ConfProperties.username);
        params.put("passwordmd5", DigestUtils.md5Hex(ConfProperties.password));

        //登录1
        String result = HttpUtils.doHttp("get", HttpUtils.loginUrl, null, params);
        //学员编号
        String xybh = result.substring(result.indexOf("XYBH") + 10, result.indexOf("\\", result.indexOf("XYBH") + 10));
        //不知道啥号，能用就行
        String jgid = result.substring(result.indexOf("JGID") + 10, result.indexOf("\\", result.indexOf("JGID") + 10));
        //又一个不知道啥的号，不管了，能用就行
        String xxzh = result.substring(result.indexOf("XXZH") + 10, result.indexOf("\\", result.indexOf("XXZH") + 10));

        //登录2,后台只判断了User-Agent。。。
        HashMap<String, String> params2 = new HashMap<>();
        params2.put("xybh", xybh);
        params2.put("password", ConfProperties.password);
        params2.put("jgid", jgid);
        HttpUtils.addJsonpParams(params2);
        HttpUtils.doHttp("get", HttpUtils.longquanjiaxiaoLoginUrl, null, params2);
        return xxzh;
    }
}
