package com.amazonaws.lambda.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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

        // temperature와 LED 값이 이전상태와 동일한 경우 테이블에 저장하지 않고 종료 
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

/**
 * AWS IoT은(는) 섀도우 업데이트가 성공적으로 완료될 때마다 /update/documents 주제에 다음 상태문서를 게시합니다
 * JSON 형식의 상태문서는 2개의 기본 노드를 포함합니다. previous 및 current. 
 * previous 노드에는 업데이트가 수행되기 전의 전체 섀도우 문서의 내용이 포함되고, 
 * current에는 업데이트가 성공적으로 적용된 후의 전체 섀도우 문서가 포함됩니다. 
 * 섀도우가 처음 업데이트(생성)되면 previous 노드에는 null이 포함됩니다.
 * 
 * timestamp는 상태문서가 생성된 시간 정보이고, 
 * device는 상태문서에 포함된 값은 아니고, Iot규칙을 통해서 Lambda함수로 전달된 값이다. 
 * 이 값을 해당 규칙과 관련된 사물이름을 나타낸다. 
 */
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