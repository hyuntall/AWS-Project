// 중지를 위해 ID 보관
var intervalId = null;
var payload;
// API 시작
function On() {

    document.getElementById("result").innerHTML="조회 중...";
 payload = { // 다음의 내용을 payload에 입력한다. 호출되는 람다함수는 timeWT라는 태그네임이 입력되었을 때에만 날씨api로부터 날씨 정보를 얻어와 디바이스 섀도우에 전송한다.
    tags : [
            {
                tagName: "timeWT",
                tagValue: "202012050200"
            }
    ]
};
    invokeAPI();
    
}
// API 중지

var invokeAPI = function() { //다음 API 주소에 payload를 전송한다.
    // 디바이스 조회 URI
    var API_URI = 'https://q1gtpjku33.execute-api.ap-northeast-2.amazonaws.com/prod/devices/MyMKRWiFi1010';
    
      
    $.ajax(API_URI, {
        method: 'PUT',
        contentType: "application/json",
        data: JSON.stringify(payload), // payload를 전송하기 전에 문자열로 바꿔준다.

        success: function (data, status, xhr) { // API에 payload 전송에 성공했을 때, 호출된 람다함수에서 얻은 현재 날씨 정보를 웹 화면에 출력한다.
                var result = JSON.parse(data);
                document.getElementById("result").innerText = "현재 기온: "+ result.state.desired.WT+"ºC";
               
                console.log("data="+data);
        },
        error: function(xhr,status,e){
                alert("error");
        }
    });
};
