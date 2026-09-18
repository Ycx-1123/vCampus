# vCampus 校园综合服务系统

vCampus 是一个基于 Java Swing、Socket 通信和 Access 数据库的校园综合服务系统。项目围绕学生、教师和管理员三类角色，提供信息门户、选课管理、图书馆、宿舍管理、校园商店、校园银行等校园业务模块，用于课程实训场景下的客户端/服务器端系统开发与联调。

## 项目特点

- 采用 C/S 架构，客户端通过 `SocketClient` 与服务端 `Server` 进行对象流通信。
- 使用统一的 `Message` 对象封装请求类型、请求数据、响应状态和响应信息。
- 基于角色加载不同功能入口，学生、教师、管理员进入系统后看到不同菜单与业务界面。
- 使用 Access 数据库 `vCampus.accdb` 保存用户、课程、图书、宿舍、商店、银行等业务数据。
- 引入 FlatLaf 优化 Swing 界面显示效果，整体界面更接近现代桌面应用。
- 服务端按模块拆分 Service、DAO、Handler，便于业务维护和扩展。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 开发语言 | Java 8 |
| 客户端界面 | Java Swing、FlatLaf |
| 通信方式 | Java Socket、ObjectInputStream、ObjectOutputStream |
| 数据库 | Microsoft Access `.accdb` |
| 数据库驱动 | UCanAccess、Jackcess、HSQLDB |
| 文档/导入支持 | Apache POI |
| 邮件支持 | JavaMail |
| 推荐 IDE | Eclipse |

## 主要功能

### 登录与主界面

- 支持学生、教师、管理员登录。
- 登录成功后记录当前用户，并在后续请求中自动附加一卡通号。
- 主界面根据用户身份和管理员权限动态加载左侧菜单。

### 信息门户 / 学籍管理

- 学生可查看个人基础信息。
- 管理员可进入学籍管理相关界面，对学生信息进行维护。

### 选课与课程管理

- 学生端支持课程选择相关功能。
- 教师端支持上课管理、班级名单查看和成绩录入。
- 管理员端支持课程管理、开放选课设置等后台操作。

### 图书馆

- 支持图书、分类、馆藏副本、借阅记录、预约记录和推荐等业务。
- 管理员可查询借阅历史并处理图书管理相关操作。

### 宿舍管理

- 学生端包含宿舍信息、申请、报修、缴费等功能入口。
- 管理员端包含宿舍、住宿、费用、报修和申请管理功能。

### 校园商店

- 商品卡片展示，支持商品图片、分类、价格、库存显示。
- 支持购物车、数量选择、多选结算、历史订单查看。
- 支持校园卡支付与银行卡支付，支付前查询余额并校验是否足够。
- 支持收藏商品、国补红包、库存实时刷新。
- 管理员可在购物模式和管理模式之间切换，进行商品上架、补货、下架和库存刷新。

### 校园银行

- 支持银行卡开户、查询、存款、取款、转账、流水查看等功能。
- 管理员可进行银行账户管理。
- 商店、宿舍等模块可复用银行卡支付能力。

## 目录结构

```text
vCampus
├── src
│   ├── vCampus
│   │   ├── client              # 客户端界面与客户端服务
│   │   ├── common              # 通信对象、枚举、实体类
│   │   └── server              # 服务端入口、业务服务、DAO、Handler
│   ├── *.jpg / *.png           # 主界面模块图标与资源
├── images                      # 商店商品图片与背景图
├── lib                         # 第三方 jar 依赖
├── bin                         # Eclipse 编译输出目录
├── vCampus.accdb               # Access 数据库文件
├── .classpath                  # Eclipse 类路径配置
└── .project                    # Eclipse 项目配置
```

## 核心类说明

| 类 | 位置 | 说明 |
| --- | --- | --- |
| `vCampus.server.Server` | `src/vCampus/server/Server.java` | 服务端入口，监听 `8888` 端口并分发业务请求。 |
| `vCampus.client.LoginFrame` | `src/vCampus/client/LoginFrame.java` | 客户端登录界面，登录成功后进入主界面。 |
| `vCampus.client.MainFrame` | `src/vCampus/client/MainFrame.java` | 客户端主框架，根据角色加载不同模块。 |
| `vCampus.client.SocketClient` | `src/vCampus/client/SocketClient.java` | 客户端统一 Socket 通信工具。 |
| `vCampus.common.Message` | `src/vCampus/common/Message.java` | 客户端与服务端之间传输的统一消息对象。 |
| `vCampus.server.dao.DBUtil` | `src/vCampus/server/dao/DBUtil.java` | 数据库连接工具类。 |
| `vCampus.server.ShopServerSrvImpl` | `src/vCampus/server/ShopServerSrvImpl.java` | 校园商店服务端业务实现。 |
| `vCampus.server.service.BankService` | `src/vCampus/server/service/BankService.java` | 校园银行业务服务。 |

## 运行环境

运行前请确认：

- 已安装 JDK 8。
- 使用 Eclipse 导入项目时，项目 JRE 设置为 `JavaSE-1.8`。
- `lib` 目录下的 jar 已加入 Build Path。
- `vCampus.accdb` 位于项目根目录。
- 服务端和客户端的 IP、端口配置一致。

当前服务端监听端口：

```java
private static final int PORT = 8888;
```

当前客户端连接配置位于 `SocketClient.java` 和 `LoginFrame.java`，联机运行时需要将其中的服务端 IP 修改为服务端电脑的真实 IPv4 地址。

## 快速启动

### 1. 导入项目

使用 Eclipse：

1. 选择 `File -> Import -> Existing Projects into Workspace`。
2. 选择项目根目录。
3. 确认 `.classpath` 中的 `lib/*.jar` 已正常加载。

### 2. 启动服务端

运行：

```text
vCampus.server.Server
```

启动成功后，控制台会显示服务端正在监听 `8888` 端口。

### 3. 启动客户端

运行：

```text
vCampus.client.LoginFrame
```

输入一卡通号和密码登录系统。

## 数据库说明

项目使用 Access 数据库 `vCampus.accdb`。常见业务表包括：

- `tbl_student`：学生信息与学生校园卡余额。
- `tbl_teacher`：教师信息与教师校园卡余额。
- `tbl_admin`：管理员信息与模块管理权限。
- `tbl_bank_account`：银行卡账户、余额、状态和支付密码。
- `tblProduct`：校园商店商品信息。
- `tblOrder`：校园商店订单主表。
- `tblOrderItem`：校园商店订单明细表。
- 图书、课程、宿舍相关表由各模块 DAO 访问维护。

数据库连接主要通过 UCanAccess 完成，例如：

```java
jdbc:ucanaccess://vCampus.accdb
```

## 通信协议

客户端向服务端发送 `Message` 对象，服务端处理后返回 `Message` 响应。

`Message` 主要字段：

| 字段 | 说明 |
| --- | --- |
| `type` | 请求类型，例如登录、查询商品、提交订单等。 |
| `data` | 请求数据，可以是实体对象、Map、List 等。 |
| `success` | 服务端处理是否成功。 |
| `responseMsg` | 响应提示或错误信息。 |
| `senderId` | 当前登录用户的一卡通号，用于会话校验。 |

## 注意事项

- 客户端启动前必须先启动服务端。
- 如果提示“网络连接失败”，请检查服务端是否启动、IP 是否正确、端口是否被占用。
- 数据库文件应放在项目根目录，否则 UCanAccess 可能无法找到 `vCampus.accdb`。
- 多人联调时，只建议修改 `SocketClient.java` 和 `LoginFrame.java` 中的服务端 IP。
- `server_operations.log` 会记录服务端操作日志，联调时可用于排查问题。

## 开发建议

- 新增模块时，建议按 `client / common / server / dao / service` 的结构分层实现。
- 新增通信请求时，建议统一维护请求类型常量，避免硬编码字符串分散在各处。
- 涉及扣款、库存、订单等一致性操作时，应在服务端事务中完成。
- 客户端只做基础输入校验，最终业务校验应以服务端为准。

## 项目状态

本项目为校园综合服务系统实训项目，适合用于 Java Swing 桌面端、Socket 通信、Access 数据库和多模块业务系统的学习与演示。
