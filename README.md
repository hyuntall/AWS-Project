# AWS-Project 
## 웨더 스타일러 / 무드 라이트
#### 현재 기온과 어제의 기온을 비교하여 LED를 통한 시각적인 연출과, SNS로 옷차림을 추천해주는 서비스
## 기능 설명
 * 웹에서 버튼 클릭 시 람다함수에서 날씨 API에 정보를 요청한다.
 * 웹에서 현재 날씨와 Dynamo DB의 기록을 확인할 수 있다.
 * 날씨 정보에 따라 LED의 색을 달리하여 표현한다.
 * 날씨 정보에 따라 이메일의 내용을 달리하여 전송한다.
 * 날씨 정보와 LED의 컬러를 Dynamo DB에 기록한다.
## 서비스 구조
![구조](https://user-images.githubusercontent.com/71054445/101979693-847bb380-3ca2-11eb-8ff5-f955f937765b.png)
## Java AWS Lambda 코드 설명
#### UpdateDeviceLambdaProject
###### 웹에서 버튼 클릭 시 날씨 API를 요청하여 디바이스 섀도우에 전송하는 함수
>> 
```javascript

// 현재 시각의 날씨 데이터를 API로 받아와서 디바이스 섀도우에 전송하는 함수
public class UpdateDeviceHandler implements RequestHandler<Event, String> {
    API api = new API();
    String nowDate = LocalDateTime.now().plusHours(8L).format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
//날씨api로부터 얻을 수 있는 온도는 1시간 전 기준이 가장 최신 정보이기 때문에 현재시간 -1시간의 시간 데이터를 불러온다.
    String nowDate2 = LocalDateTime.now().minusHours(14L).format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
//날씨api에서 24시간 전의 온도데이터까지만 제공을 하기 때문에 에러율을 줄이기 위해 22시간 전의 시간 데이터를 불러올 수 밖에 없었다.
          
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
            if(tags.get(i).tagName.equals("timeWT")) {
            // timeWT라는 태그네임을 입력받았을 때에만 날씨api를 호출하여 문자열에 추가한다.
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
```
#### RecodingDeviceDataJavaProject2
###### 디바이스 섀도우의 데이터를 DynamoDB에 전송하는 함수
>> 
```javascript
// 디바이스 섀도우의 데이터를 DynamoDB에 저장하는 함수
public class RecordingDeviceInfoHandler2 implements RequestHandler<Document, String>  {
	static String buffer;
    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "Logging";

    @Override
    public String handleRequest(Document input, Context context) {
        this.initDynamoDbClient();
        context.getLogger().log("Input: " + input);

        //return null;
        return persistData(input);
    }

    private String persistData(Document document) throws ConditionalCheckFailedException {

        // Epoch Conversion Code: https://www.epochconverter.com/
        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        String timeString = sdf.format(new java.util.Date (document.timestamp*1000));

        // WT와 YesterDay, LED 값이 이전상태와 동일한 경우 테이블에 저장하지 않고 종료 
        if (document.current.state.desired.WT.equals(document.previous.state.desired.WT) && 
                document.current.state.desired.YesterDay.equals(document.previous.state.desired.YesterDay)&& 
                document.current.state.desired.COLOR.equals(document.previous.state.desired.COLOR)) {
                return null;
        }
        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(new PutItemSpec().withItem(new Item().withPrimaryKey("deviceId", document.device)
                        .withLong("time", document.timestamp)
                        .withString("WT", document.current.state.desired.WT)
                        .withString("YesterDay", document.current.state.desired.YesterDay)
                        .withString("COLOR", document.current.state.desired.COLOR)
                        .withString("timestamp",timeString)))
                .toString();
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion("ap-northeast-2").build();

        this.dynamoDb = new DynamoDB(client);
    }
}

class Document {
    public Thing previous;       
    public Thing current;
    public long timestamp;
    public String device;       // AWS IoT에 등록된 사물 이름 
}

class Thing {
    public State state = new State();
    public long timestamp;
    public String clientToken;

    public class State {
        public Tag reported = new Tag();
        public Tag desired = new Tag();

        public class Tag {
            public String temperature;
            public String LED;
            public String COLOR;
            public String WT;
            public String timeWT;
            public String YesterDay;
        }
    }
}
```
#### LogDeviceLambdaJavaProject
###### DynamoDB에 기록된 LED의 상태와 날씨 정보의 기록을 조회하는 함수
>>
```javascript
public class LogDeviceHandler implements RequestHandler<Event, String> {

    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "Logging";

    @Override
    public String handleRequest(Event input, Context context) {
        this.initDynamoDbClient();

        Table table = dynamoDb.getTable(DYNAMODB_TABLE_NAME);

        long from=0;
        long to=0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

            from = sdf.parse(input.from).getTime() / 1000;
            to = sdf.parse(input.to).getTime() / 1000;
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("deviceId = :v_id and #t between :from and :to")
                .withNameMap(new NameMap().with("#t", "time"))
                .withValueMap(new ValueMap().withString(":v_id",input.device).withNumber(":from", from).withNumber(":to", to)); 

        ItemCollection<QueryOutcome> items=null;
        try {           
            items = table.query(querySpec);
        }
        catch (Exception e) {
            System.err.println("Unable to scan the table:");
            System.err.println(e.getMessage());
        }

        return getResponse(items);
    }

    private String getResponse(ItemCollection<QueryOutcome> items) {

        Iterator<Item> iter = items.iterator();
        String response = "{ \"data\": [";
        for (int i =0; iter.hasNext(); i++) {
            if (i!=0) 
                response +=",";
            response += iter.next().toJSON();
        }
        response += "]}";
        return response;
    }

    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

        this.dynamoDb = new DynamoDB(client);
    }
}

class Event {
    public String device;
    public String from;
    public String to;
}
```
#### MonitoringLambda
###### 날씨 요청 시 현재 날씨와 어제의 날씨를 비교하고, 날씨에 따른 추천 옷차림을 메일로 전송하는 함수
>>
```javascript
// 날씨 데이터를 요청받으면 현재의 날씨와 추천하는 옷 스타일을 이메일로 전송하는 함수
public class Monitoring implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
	    context.getLogger().log("Input: " + input);
	    String json = ""+input;
	    JsonParser parser = new JsonParser();
	    JsonElement element = parser.parse(json);
	    JsonElement state = element.getAsJsonObject().get("state");
	    JsonElement desired = state.getAsJsonObject().get("desired");
	    String WT = desired.getAsJsonObject().get("WT").getAsString();
	    String YesDay = desired.getAsJsonObject().get("YesterDay").getAsString();
	    double today = Double.valueOf(WT);
	    double yesterday = Double.valueOf(YesDay);
	    
	    final String AccessKey="AKIARRXCMQZVBQPRH3JF";
        final String SecretKey="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";
	    final String topicArn="arn:aws:sns:ap-northeast-2:106775217770:temerature_warning_topic";

	    BasicAWSCredentials awsCreds = new BasicAWSCredentials(AccessKey, SecretKey);  
	    AmazonSNS sns = AmazonSNSClientBuilder.standard()
	                .withRegion(Regions.AP_NORTHEAST_2)
	                .withCredentials( new AWSStaticCredentialsProvider(awsCreds) )
	                .build();
		// 기존에 실습에서 배운 온도가 특정값 이상일 때 이메일이 전송되는 것과 달리, 날씨 정보를 받아올 때마다 이메일을 전송한다.
		// 단, 현재의 날씨와 어제의 날씨를 비교하여 전송하는 문자열을 달리하고, 현재 날씨의 온도에 따라서 문자열을 달리하여 추가한다.
	    String msg = "오늘의 기온은 " + WT + "C 입니다.\n";
	    final String subject = "오늘의 날씨";
	    
	    if (today > yesterday ) {
	    	msg += "오늘은 어제보다 기온이 높습니다.\n";
	        
	    }
	    else if (today < yesterday ) {
	    	msg += "오늘은 어제보다 기온이 낮습니다.\n";
	        
	    }
	    else {
	    	msg += "오늘은 어제와 기온이 같습니다.\n";
	        
	    }
	    if(today >= 9) {
	    	msg += "오늘은 트렌치 코트나 가벼운 점퍼같은게 좋겠네요.";
	    }
	    else if(today >= 5 && today <9) {
	    	msg += "코트나 히트텍을 입는건 어떨까요?.";
	    }
	    else if(today < 5) {
	    	msg += "오늘은 날씨가 추우니 패딩이나 두꺼운 코트를 입는걸 추천드려요.";
	    }
	    PublishRequest publishRequest = new PublishRequest(topicArn, msg, subject);
        PublishResult publishResponse = sns.publish(publishRequest);

	    return subject+ "temperature = " + WT + "!";
	}


}
```
## 시연 모습
<img src="https://user-images.githubusercontent.com/71054445/101980110-6d3ec500-3ca6-11eb-954d-7936b01c11a9.png" width="50%">

> 위에 조회 시작 버튼을 누르면 UpdateDeviceHandler 함수에 "TimeWT"라는 태그네임의 문자열이 전송된다.
>> ( UpdateDeviceHandler 함수는 "TimeWT"라는 태그네임을 입력받으면 날씨 API를 요청한다. )
<img src="https://user-images.githubusercontent.com/71054445/101980523-5a79bf80-3ca9-11eb-86dc-c6f22c939137.png" width="50%">

> 실시간 날씨 정보를 요청하여 받아온 모습이다.
<img src="https://user-images.githubusercontent.com/71054445/101980702-9eb98f80-3caa-11eb-80bf-c5447131677e.png" width="80%">

> 개발자 도구를 통해 다음 문자열이 응답되었음을 확인할 수 있다.
>> ( "TimeWT"라는 태그네임을 전송하도록 코드를 작성해두었다. "TimeWT"의 태그네임을 입력받으면 값에 상관없이 날씨 API가 호출된다. )
<img src="https://user-images.githubusercontent.com/71054445/101980795-5a7abf00-3cab-11eb-890e-b24dc07f0f5c.png" width="80%">

> 아래의 기록 조회 시작 버튼을 클릭 시 LED 상태와 날씨정보에 대한 기록이 출력된다
>> ( 단, LED 상태는 아두이노가 연결되어있을 때에만 갱신된다. )
<img src="https://user-images.githubusercontent.com/71054445/101980884-07553c00-3cac-11eb-9e81-99af57c7d2e4.jpg" width="50%">

> 현재의 날씨가 어제보다 낮기 때문에 BLUE라는 값과 함께 LED가 파란색으로 점등된 모습이다.
<img src="https://user-images.githubusercontent.com/71054445/101980943-6dda5a00-3cac-11eb-9836-76c44d6402f5.png" width="70%">

> 현재와 어제의 날씨의 비교 정보와 온도에 따른 추천 옷차림이 이메일로 전송된 모습이다.
