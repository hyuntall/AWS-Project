package com.amazonaws.lambda.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClientBuilder;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonCreator;

// 현재 시각의 날씨 데이터를 API로 받아와서 디바이스 섀도우에 전송하는 함수
public class UpdateDeviceHandler implements RequestHandler<Event, String> {
    API api = new API();
    String nowDate = LocalDateTime.now().plusHours(8L).format(DateTimeFormatter.ofPattern("yyyyMMddHH")); //날씨api로부터 얻을 수 있는 온도는 1시간 전 기준이 가장 최신 정보이기 때문에 현재시간 -1시간의 시간 데이터를 불러온다.
    String nowDate2 = LocalDateTime.now().minusHours(14L).format(DateTimeFormatter.ofPattern("yyyyMMddHH")); //날씨api에서 24시간 전의 온도데이터까지만 제공을 하기 때문에 에러율을 줄이기 위해 22시간 전의 시간 데이터를 불러올 수 밖에 없었다.
          
    @Override
    public String handleRequest(Event event, Context context) {
        context.getLogger().log("Input: " + event);
        AWSIotData iotData = AWSIotDataClientBuilder.standard().build();
        String payload = getPayload(event.tags);

        UpdateThingShadowRequest updateThingShadowRequest  = 
                new UpdateThingShadowRequest()
                    .withThingName(event.device)
                    .withPayload(ByteBuffer.wrap(payload.getBytes()));

        UpdateThingShadowResult result = iotData.updateThingShadow(updateThingShadowRequest);
        byte[] bytes = new byte[result.getPayload().remaining()];
        result.getPayload().get(bytes);
        String resultString = new String(bytes);
        return resultString;
    }

    private String getPayload(ArrayList<Tag> tags) { 
        String tagstr = "";
        String time, date, datetime, date2, time2;
        for (int i=0; i < tags.size(); i++) {
            if (i !=  0) tagstr += ", ";
            if(tags.get(i).tagName.equals("timeWT")) { // timeWT라는 태그네임을 입력받았을 때에만 날씨api를 호출하여 문자열에 추가한다.
            	try {
            		date = nowDate.substring(0,8);
            		time = nowDate.substring(8,10);
            		date2 = nowDate2.substring(0,8);
            		time2 = nowDate2.substring(8,10);
            		String strUrl = "http://apis.data.go.kr/1360000/VilageFcstInfoService/getUltraSrtNcst?ServiceKey=BpsM0AXqnAU3tmf4aGQtuG6UB8gkScP3SkYOs8O9Yw%2Fon5PthRTvlQ7dUzzEqUCddMs%2FYFnwXHZ6Ft5X6sJIng%3D%3D&pageNo=1&numOfRows=10&dataType=JSON&base_date="+date+"&base_time="+time+"00&nx=59&ny=126\n";
            		String strUrl2 = "http://apis.data.go.kr/1360000/VilageFcstInfoService/getUltraSrtNcst?ServiceKey=BpsM0AXqnAU3tmf4aGQtuG6UB8gkScP3SkYOs8O9Yw%2Fon5PthRTvlQ7dUzzEqUCddMs%2FYFnwXHZ6Ft5X6sJIng%3D%3D&pageNo=1&numOfRows=10&dataType=JSON&base_date="+date2+"&base_time="+time2+"00&nx=59&ny=126\n";
					tagstr += String.format("\"%s\" : \"%s\"", "WT", api.get(strUrl));
					tagstr += ", ";
					tagstr += String.format("\"%s\" : \"%s\"", "YesterDay", api.get(strUrl2));
					tagstr += ", ";
					tagstr += String.format("\"%s\" : \"%s\"", "NowTime", nowDate);
					tagstr += ", ";
					tagstr += String.format("\"%s\" : \"%s\"", tags.get(i).tagName, tags.get(i).tagValue);
					 
				} catch (IOException | ParseException | org.json.simple.parser.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            else {
            	tagstr += String.format("\"%s\" : \"%s\"", tags.get(i).tagName, tags.get(i).tagValue);
            }
        }
        return String.format("{ \"state\": { \"desired\": { %s } } }", tagstr);
    }
    
}

class Event {
    public String device;
    public ArrayList<Tag> tags;

    public Event() {
         tags = new ArrayList<Tag>();
    }
}

class Tag {
    public String tagName;
    public String tagValue;

    @JsonCreator 
    public Tag() {
    }

    public Tag(String n, String v) {
        tagName = n;
        tagValue = v;
    }
}
class API{ // 날씨API를 요청하는 함수
	String b;
    
    public String get(String strUrl) throws IOException, ParseException, org.json.simple.parser.ParseException {
    	   URL url = new URL(strUrl);
    	    System.out.println(url);
    	           HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	    conn.setRequestMethod("GET");
    	    conn.setRequestProperty("Content-type", "application/json");
    	    System.out.println("Response code: " + conn.getResponseCode());
    	    BufferedReader rd;
    	    if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
    	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    	    } else {
    	        rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
    	    }
    	    StringBuilder sb = new StringBuilder();
    	    String line;
    	    while ((line = rd.readLine()) != null) {
    	        sb.append(line);
    	    }
    	    rd.close();
    	    conn.disconnect();
    	    JSONParser parser = new JSONParser(); // 파싱 작업을 하기 위한 객체를 만들어 줌
    	    JSONObject obj = (JSONObject)parser.parse(sb.toString()); // String json을 파싱받은 뒤, JSONObject 형태로 저장함
    	    JSONObject response = (JSONObject)obj.get("response");
    	    JSONObject body = (JSONObject)response.get("body");
    	    JSONObject items = (JSONObject)body.get("items");
    	    JSONObject tt = (JSONObject) parser.parse(items.toString());
    	       JSONArray jsonArray = (JSONArray) tt.get("item");
    	       for(int i=0; i<jsonArray.size(); i++) {
    	          JSONObject objectInArray = (JSONObject) jsonArray.get(i);
    	          
    	          String a = (String) objectInArray.get("category");
    	          if(a.equals("T1H")) {
    	             b = (String) objectInArray.get("obsrValue");
    	          }
    	       }
    	       return b;
    	}
}
