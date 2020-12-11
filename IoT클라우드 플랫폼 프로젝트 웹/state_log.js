// 중지를 위해 ID 보관
var intervalId = null;
function getToday(){
    var date = new Date();
    var year = date.getFullYear();
    var month = ("0" + (1 + date.getMonth())).slice(-2);
    var day = ("0" + date.getDate()).slice(-2);

    return year + "-" + month + "-" + day;
}
// API 시작
function Start() {
    document.getElementById("result2").innerHTML="조회 중...";
    invokeAPI2();
    intervalId = setInterval(invokeAPI2, 1000);
    
}
// API 중지

function Stop() {
    if(intervalId != null) {
        clearInterval(intervalId);
        document.getElementById("result2").innerHTML="No Data";
    }
}

var invokeAPI2 = function() {
    // 디바이스 조회 URI
    // prod 스테이지 편집기의 맨 위에 있는 "호출 URL/devices"로 대체해야 함
    var API_URI = 'https://0yrz85sv48.execute-api.ap-northeast-2.amazonaws.com/prod/devices/MyMKRWiFi1010/log?from=2020-11-29%2000:00:00&to='+getToday()+'%2023:59:59';                 
    $.ajax(API_URI, {
        method: 'GET',
        contentType: "application/json",
        success: function (result) {
            Data = JSON.parse(result);
            printData(Data.data);    
        },
        error: function(xhr,status,e){
                alert("error");
        }
    });
};

// 데이터 출력을 위한 함수
var printData = function (result) {
        
        if (document.getElementsByTagName("div")[0] != null)
            document.getElementsByTagName("div")[0].remove();

        $("#result2").empty();
        
        var tr = document.createElement("tr");
        var th1 = document.createElement("th");
        var th2 = document.createElement("th");
        var th3 = document.createElement("th");
        var th4 = document.createElement("th");
        var td1 = document.createElement("td");
        $("#result2").append(tr);
        if (result.length > 0) {

            var tr = document.createElement("tr");
            tr.setAttribute("style","background-color:#01DFD7");

            var th1 = document.createElement("th");
            th1.innerHTML = "LED COLOR";
            var th2 = document.createElement("th");
            th2.innerHTML = "Temperature";
            var th3 = document.createElement("th");
            th3.innerHTML = "YesterDay";
            var th4 = document.createElement("th");
            th4.innerHTML = "Time";
            tr.append(th1);
            tr.append(th2);
            tr.append(th3);
            tr.append(th4);
            result = result.reverse();
            $("#result2").append(tr);
            result.forEach(function(v){
                var tr = document.createElement("tr");
                var td1 = document.createElement("td");
                var td2 = document.createElement("td");
                var td3 = document.createElement("td");
                var td4 = document.createElement("td");
                td1.innerText = v["COLOR"];
                td2.innerText = v["WT"]+"ºC";
                td3.innerText = v["YesterDay"]+"ºC";
                td4.innerText = v["timestamp"];
                tr.append(td1);
                tr.append(td2);
                tr.append(td3);
                tr.append(td4);
                $("#result2").append(tr);

            })
        } 
}