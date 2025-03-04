# 소켓 통신

#### 미니 톰캣을 구현해보는 프로젝트를 진행하던 중, Socket을 자바에서 생성할 수 있다는 것을 알게 된 후, “톰캣”은 Application Layer위에서 동작하는데 어떻게 소켓을 톰캣에서 생성할 수 있는거지? 라는 궁금증이 생겼다. 그 궁금증을 해소하기 위해 소켓과 여러 자료를 보면서 내가 소켓에 대해 잘못 이해했음을 깨닫고 이를 공부하였다. 아래의 내용은 내가 공부한 내용과 위의 질문에 대한 답들이다.

---

## 먼저, 소켓이란 무엇인지 알아보자
#### 우리가 생성한 소켓은 IP+Port주소, 클라이언트 요청/응답을 담는 버퍼, 네트워크 상태 정보 등을 가지고 있는 추상화 객체이다.[^1] 소켓에 관한 여러 변수와 메서드들은 무엇이 있는지 찾아 보면 다음과 같은것을을 볼 수 있다.

### Listen 함수
- <img width="1200" alt="image" src="https://github.com/user-attachments/assets/9950d816-dcb3-4fab-8a80-e6fef1f12692" />
### rcv_nxt : 다음에 받을것으로 예상되는 데이터 시퀀스 넘버
- <img width="1200" alt="image" src="https://github.com/user-attachments/assets/4c4d9d5b-39c3-4020-bb55-e09f2a14ab9c" />
### Sk_buff_head : 소켓의 버퍼 헤드 
- <img width="1200" alt="image" src="https://github.com/user-attachments/assets/3f1d51b3-2280-474f-b3fd-d48f64c87aaa" />


#### 이 외에도 다양한 것들이 있다. 추가로 앞서 내가 소켓을 추상화 객체라고 했던 이유는 소켓을 이용해 통신을 할때 소켓과 관련한 시스템 콜을 호출을 하면 직접 구현할 필요 없이 해당 기능을 사용할 수 있기 때문이다.[^2]

---

# 소켓을 왜 사용해 통신을 할까?

#### 이렇게 소켓은 다른 컴퓨터 안에 있는 프로세스들 간의 통신에 있어서 사용된다. 어떤 이유에서일까? 먼저Os안에 소켓을 다룰수 있는 시스템 콜을 이미 만들어 두어서 우리는 쉽게 사용만 하면 되기 때문이다. Os가 읽을 수 있는 클라이언트 요청과 응답 버퍼를 소켓을 사용하지 않고 우리가 직접 만들게 된다면, 추가적으로 os가 읽어가고 적는 시스템 콜도 만들고 식별자도 만들어야 할것이다. 이 복잡한 과정을 추상화하여 편리하게 네트워크 통신을 할 수 있도록 만든것이 소켓과 관련 시스템 콜들이다.

또한 클라이언트의 요청 패킷을 소켓 버퍼에 포인터로 담음으로써 OS가 클라의 요청 값을 일일이 복사해 사용하지 하지 않고 클라 요청 정보가 필요할때마다 버퍼에 접근해 빠르게 접근해 사용할 수 있다. 추가적으로 버퍼에 담는 이 행위를 통해 App에서 더이상 받을 수 없는 상황에 클라 요청데이터가 유실되지 않도록 하게 함으로써 안정성을 높히는 효과도 있다.
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/850b4792-84ed-4839-a7ba-706e4d8c69ff" />
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/99dbdf4a-ec46-436b-a6a1-ffb7eedec79f" />[^1]
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/b2a10cc3-6148-4f0b-b1a3-dfdb1a2d55d8" />[^3]


---

그럼 소켓 시스템 콜 메서드들이 동작하는 순서를 알아보자. 먼저 `socket()`을 호출하여 새로운 객체를 생성한다.

```c
int s = socket(int domain, int type, int protocol)
```

그리고 `bind()`를 호출하여 소켓의 고유한 이름을 만들어준다. 
```c
int error = bind(int s, const struct sockaddr *addr, socklen_t addrlen)
```


그리고 `listen()`를 호출하여 소켓의 고유한 이름을 만들어준다. 
```c
int error = listen(int s, int backlog)
```

이후 `listen()` 함수를 호출하여서 해당 소켓으로 클라이언트의 요청을 받겠다는 것을 설정하고
```c
int error = bind(int s, const struct sockaddr *addr, socklen_t addrlen)
```

이후 `accept()`를 호출한다. 이는 실제 클라이언트의 연결 요청을 받을때까지 블락킹으로 진행된다.
```c
int snew = accept(int s, clientaddr, &clientaddrlen);
```
이후 클라이언트에서 DNS와 같은 곳으로부터 서버 프로세스의 IP와 prot주소 가져와 `connect()`를 호출한다. 
```c
int error = connect(int s, const struct sockaddr *serveraddr, socklen_t serveraddrlen)
```
그럼 TCP의 경우 3 ways handshake가 일어나서 connection을 논리적으로 맺게 될것이다. 그럼 이 연결 이후 `Read()` 시스템콜과 `write()`시스템 콜을 이용하여 데이터를 실제로 주고 받게 될것이다. 


데이터를 다 주고 받았으면 `shutdown(int socket, int how)`을 호출하여 연결을 논리적으로 끊는다. 직접 소켓을 없애는 방법은 `close()`시스템 콜을 호출하면 된다.

### 위 과정을 그림으로 표현하면 아래와 같다. 
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/734fd98c-3988-4e5c-ad26-d0a34610aed5" />

---

# 톰캣 프로세스

그럼 이제 톰캣에서 어떻게 이 new Socket과 같은 코드를 적을 수 있었는지를 알아보자. 답부터 말하면 사실 톰캣은 직접 소켓을 생성할 수 없다. (소켓을 생성 관리하는 것은 시스템 콜로 가능하다) 다만 해당 코드를 실행하면 내부적으로 시스템콜을 호출하여 os가 요청한 기능을 실행하게 한다. 그럼 이제 위의 과정을 상기하며 각각의 Socket관련 함수가 내부적으로 어떤 시스템 콜을 호출하는지 알아보자. 
먼저 
```java
ServerSocket serverSocket = new ServerSocket(9000);
```

이 코드의 의미는 컴퓨터 IP + 9000포트에오는 클라이언트의 요청을 받겠다는 의미이다. 그럼 내부적으로 
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/d85f23ca-6f51-414d-b1d5-5c62005de0f5" />
이런 로직을 거쳐 `bind()`, `listen()` 시스템 콜을 호출한다. 
이후
```java
Socket clientSocket = serverSocket.accept();
```
을 호출해 accept()시스템 콜을 호출한다. 이는 앞서 보았듯이 블락킹 메서드이다. 따라서 클라이언트의 요청 연결이 올때까지 블락킹된다. 

<img width="1200" alt="image" src="https://github.com/user-attachments/assets/a13d2bef-9d1c-41d5-bb50-482a8ae5f801" />
클라의 요청이 오게 된다면 clientSocket 을 만들고 clientsocket에서 getinputstream메서드를 호출하여 소켓 버퍼의 접근을 위한 준비를 한다.

이제 반복문을 돌며 유저의 요청이 오기를 기다리고 유저의 요청이 온다면 os의 시스템콜로 read 버퍼에 담기게 될것이다. 그럼 readline()함수를 이용해 read()시스템콜을 호출해 가져와 해당 데이터를 thread를 생성해 전달한다.
### 전달할때 해당 스레드에 소켓의 정보도 넘겨준다. (결과값을 받환받으면 적절한 소켓 버퍼에 담아야 함으로)
<img width="1200" alt="image" src="https://github.com/user-attachments/assets/a87bb026-3fe5-4dc7-a8e0-f3835897a356" />
그럼 결과를 받으면 

<img width="1200" alt="image" src="https://github.com/user-attachments/assets/d02da336-c562-4a27-a1bc-1cf2c2e4925c" />
getOutputStream()을 호출해서 쓰기 버퍼에 적을 준비를 하고 write() 시스템 콜을 호출하여 결과를 써서 보낸다.


---
## 톰캣 전체 흐름 구조도 한눈에 보기 [https://giyeontomcatflow.my.canva.site/]







[^1]:[출처 : https://people.cs.rutgers.edu/~pxk/416/notes/16-sockets.html] 
[^2]:[출처 : https://elixir.bootlin.com/linux/v4.8/source/include/net/sock.h#L344] 
[^3]:[출처 : https://itnext.io/optimizing-large-file-transfers-in-linux-with-go-an-exploration-of-tcp-and-syscall-ebe1b93fb72f]] 
