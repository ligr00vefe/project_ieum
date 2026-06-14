
- Chrome 일반 창 + Chrome 시크릿 창

- 서로 다른 브라우저 프로필 2개

브라우저 A에서 아래 계정으로 로그인한다.

아이디: d01@test.com
비밀번호: 1

로그인 후 아래 주소로 이동한다.

http://localhost:8080/chat/conversations/1

브라우저 B에서 아래 계정으로 로그인한다.

아이디: c01@test.com
비밀번호: 1

로그인 후 같은 주소로 이동한다.

http://localhost:8080/chat/conversations/1

브라우저 A에서 메시지를 전송한다.

브라우저 B 화면에 새로고침 없이 메시지가 표시되면 실시간 수신이 정상 동작하는 것이다.

반대로 브라우저 B에서 메시지를 전송한다.

