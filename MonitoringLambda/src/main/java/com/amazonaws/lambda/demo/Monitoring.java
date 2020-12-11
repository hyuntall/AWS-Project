package com.amazonaws.lambda.demo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
