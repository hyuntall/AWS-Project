// 중지를 위해 ID 보관
var intervalId = null;
var payload;
// API 시작
function On() {

    document.getElementById("result").innerHTML="조회 중...";
 payload = {
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

var invokeAPI = function() {
    // 디바이스 조회 URI
    var API_URI = 'https://q1gtpjku33.execute-api.ap-northeast-2.amazonaws.com/prod/devices/MyMKRWiFi1010';
    
      
    $.ajax(API_URI, {
        method: 'PUT',
        contentType: "application/json",
        data: JSON.stringify(payload),

        success: function (data, status, xhr) {
                var result = JSON.parse(data);
                document.getElementById("result").innerText = "현재 기온: "+ result.state.desired.WT+"ºC";
               
                console.log("data="+data);
        },
        error: function(xhr,status,e){
                alert("error");
        }
    });
};
