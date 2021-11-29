앱 이름[레인노티]

실행환경
-buildToolsVersion: 30.0.3
-compileSdkVersion: 30 (Android 10 Q)
-targetSdkVersion: 30 (Android 10 Q)
-packageName: com.dongholab.rainoti
-kotlinVersion: 1.5.31 (코틀린 프로젝트입니다)

구현내용
-서비스 활성화 액티비티(MainActivity)
*레이아웃은 ConstraintLayout로 구성
*앱 실행시 서비스 실행여부를 체크하여 설정중엔 알림 트리거가 동작하지 않도록 구성
*날씨정보와 위치정보를 전달받지 못하게 되면 N/A라고 표기되어 서비스가 잘못돌아가는지 확인할 수 있습니다.
*서비스 활성화시 브로드캐스트 리시버를 통해 서비스에서 주기적으로 전달해주는 위치정보와 위치정보가 업데이트 되었을 때 날씨정보 API를 호출하여 액티비티로 전달받습니다.
*날씨 아이콘은 오픈소스 라이브러리인 weathericonview를 사용하여 날씨 아이콘을 실제 날씨에 맞게 렌더링합니다.

-Foreground 로케이션 서비스(LocationService)
*생성자로 Notification 서비스, Connectivity 서비스, Vibrate 서비스를 정의하였습니다.
*매 30초 ~ 2분 간격으로 실시간 위치정보를 높은 정확도로 받아옵니다.
*앱자체가 활성화되어있지 않으면 안드로이드 N 이후로는 자동으로 절전모드로 전환시켜 서비스가 동작할 수 없기에 notification 채널을 등록하여 상단바에 표기합니다.
*Manifast에 android:foregroundServiceType="location" 옵션을
*위치정보가 업데이트 되었을 때 날씨 정보는 Retrofit이라는 Square사의 안드로이드 오픈소스 라이브러리를 사용하여 openweathermap API를 통해 받아옵니다.
*API의 파싱은 JSON정보로 전달받기 때문에 Google에서 만든 JSON 라이브러리인 GSON을 사용하여 Kotlin Data Class로 변환하여 읽어들입니다.
*현재 날씨가 비가 오는 날씨로 저장되어 있을 떄, Wi-Fi가 끊기게 되면 비가오는 날 집 밖으로 나간것으로 판단하고 우산을 챙기라는 알림을 사용자에게 제공합니다.