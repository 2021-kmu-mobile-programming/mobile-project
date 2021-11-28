실행환경
-buildToolsVersion: 30.0.3
-compileSdkVersion: 30 (Android 10 Q)
-targetSdkVersion: 30 (Android 10 Q)
-packageName: com.dongholab.shop
-kotlinVersion: 1.5.31 (코틀린 프로젝트입니다)

구현내용
-첫번쨰화면(로그인, RelativeLayout)
*계정을 로그인해서 상품조회 가능
*비회원 상태에서도 바로 상품조회 가능
*preference를 통항 loginRepository 생성하여 사용
*이메일 체크, 패스워드 5자리 이상 체크

*회원가입 버튼 클릭시 회원가입 창 이동
-두번째화면(회원가입, LinearLayout)
*RegisterFormState로 각 폼의 값들이 변화할 때마다 회원가입 동작이 진행될 수 있는지 상태를 체크
*이미 가입된 계정이라면 스낵바를 통해 이미 존재하는 계정이라고 알려주며 로그인 창으로 바로 이동 가능
*회원가입 후 로그인 창으로 자동으로 이동
*preference로 저장하여 사용
*이메일 체크, 패스워드 5자리 이상 체크, 이름 1자리 이상 체크

-상품목록(상품조회/상품추가/회원정보, ConstraintLayout)
*프래그먼트로 구성 ShopList/ShopAdd/Account 개별 프래그먼트
*ShopActivity 내에 FloatingActionButton(FAB)을 통해 클릭시 "계정정보 버튼"과 "제품 추가" FAB서브버튼이 애니메이팅되어 표시
*로그인하지 않고 들어온 사용자의 경우 회원가입 버튼이 존재하는 Snackbar 표시하여 회원가입으로 바로 이동할 수 있음
*ShopAdd에서 암시적 인텐트로 갤러리를 호출하여 이미지를 가져와 savedStateHandle를 통해 이전 프래그먼트 정보의 State를 업데이트
*위 전달에서 Uri의 데이터 전달에 문제가 발생할 수 있기에 ShopItem에 Parcel 객체를 상속하여 ObjectMapping 적용
*ShopList에서 LiveData observer를 통해 새로운 값을 전달받으면 newItem을 통해 목록에서 값 추가
*각 상품의 아이콘은 이미지 메모리 관리를 위해 Glide 라이브러리 사용
*ShopList에서 상품을 길게 누르면 제거