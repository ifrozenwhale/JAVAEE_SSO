# JAVAEE_SSO

Finish a sso system.
CQU JAVAEE course, lab1, Single Sign on.
Team member:  Runze Tian, Dadong Jiang, Jiaxuan Cai

## 1.登录

1. 访问/clientA，判断session['isLogin']是否为null，如果是，表示本地没有登录，否则表示已经登录。

2. 如果session['isLogin']是null，redirect到/server，并携带跳转之前的路由url。

3. server的filter进行拦截，首先判断cookies中有无key为CAS-TGC的cookie，如果有，拿到其value（TGTId)，根据TGTId从HashMap（缓存）中查找TGT，如果查到，表示已经登录；否则表示未登录。如果cookies中没有key为CAS-TGC的cookie，表示未登录。

4. 如果未登录，跳转到/server/index.jsp进行登录，输入账号密码，从数据库中查询匹配，如果成功，登录成功。

5. 登录成功或者已经登录后，使用随机唯一的uuid生成TGC（TGC-xxxxxxxxxx），存储到cookie中，key为’CAS-TGC'。TGC的value同时也是TGTId。

   并同样生成随机的ST（Service Ticket），格式为'ST-xxxxxxxxxxxx'，并进行缓存，key为username，value为ST。以TGTId为key，TGT对象为value，使用HashMap进行存储。

   其中TGT对象又使用了一个HashMap，叫serviceMap记录了请求登录的client的url，以及对应的ST。这样做的目的是方便登出时，能通知所有的客户端都进行退出操作

6. 然后重定向回service url，并携带ST。这样做的目的是传递ST进行单次校验。

7. clientA的filter进行拦截，拿到参数ST，使用httpClient发送到/service/ticket的post请求，进行ST的校验。

8. server对ST进行校验。从Cache中找到ST对应的username并返回。返回前进行ST的删除。

9. clientA的post请求的响应后，拿到username，如果不为空，表示校验成功，完成登录。并建立session，设置session['isLogin']为true，session['username']为拿到的username。并缓存（ST，session）到HashMap中。这样做的目的是管理session和ST的对应关系。

10. 访问/clientB，同样进行判断session['isLogin']。如果为空，则重定向到/server进行请求，此时clientA已经在server进行了登录，cookie中记录了TGC，对应TGC找到TGT，生成ST，携带ST重定向回clientB，clientB同理拿到ST，再次请求server进行ST的校验，如果成功，则直接可以访问ClientB。否则，还需要重新登录。

 

## 2.登出

1. clientA申请退出，此时不携带参数ST，表示主动退出，则重定向/server/logout。

2. server/logout从cookies中找到CAS-TGC，从缓存中查询TGT，然后对TGT.serviceMap进行Map的遍历。对于map中的每一个（ST，serviceUrl），使用httpClient发送各自的post请求，并携带参数ST。删除TGC、TGT等。然后对response重定向到logoutSuccess页面。

3. 各个client对post请求进行响应。拿到参数ST，根据ST从缓存中拿到ST对应的session，remove掉session['isLogin']，并设置session失效，再次刷新时即退出。其中clientA作为主动申请退出的客户端，退出后自动跳转到logoutSuccess页面。

