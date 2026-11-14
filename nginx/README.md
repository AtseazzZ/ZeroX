# ZeroX 本地应用部署：Nginx 配置与操作

## 6. 日常管理

```powershell
# 修改 nginx.conf 后，先检查配置，再平滑重载
.\nginx\manage.ps1 -Action Reload

# 查看本项目 Nginx 是否正在运行
.\nginx\manage.ps1 -Action Status

# 平滑停止：等待正在处理的请求结束
.\nginx\manage.ps1 -Action Stop

# 停止后或电脑重启后，再次启动
.\nginx\manage.ps1 -Action Start

## 1. 为什么需要 Nginx

ZeroX 的部署接口把生成文件复制到 `tmp/code_deploy/{deployKey}/`，再返回
`http://localhost/{deployKey}/`。文件复制成功不代表已经有 HTTP 服务提供这些文件。

本次排查发现：部署文件存在，但 `localhost:80` 连接被拒绝。Nginx 的作用就是接收
80 端口的请求，并返回部署目录中的文件。

## 2. 检查与安装位置

电脑已经安装 Nginx，本次复用：

```text
D:\develop\nginx-1.22.0-web\nginx.exe
```

启动前确认 80 端口没有占用，也没有运行中的 Nginx。原安装目录中的配置没有修改。
ZeroX 独立使用本目录下的 `conf/nginx.conf`，运行日志和 PID 存在 `logs/`。

## 3. 配置如何把 URL 映射到文件

核心配置位于 `conf/nginx.conf`：

```nginx
listen 127.0.0.1:80;
listen [::1]:80;
server_name localhost;
root D:/newCode/JavaVueAI/ZeroX/tmp/code_deploy;
index index.html;

location / {
    try_files $uri $uri/ =404;
}
```

- `listen`：同时监听 IPv4、IPv6 的本机 80 端口。因此 `localhost` 可以访问，局域网其他电脑不能访问。
- `server_name`：当前站点使用的主机名。
- `root`：网站文件的根目录；Nginx 将 URL 路径追加在这个目录后。
- `index`：访问目录时默认返回 `index.html`。
- `try_files`：依次检查文件和目录，都不存在时返回 404。
- `mime.types`：根据扩展名返回正确的内容类型，使 CSS、JS 等资源正常加载。本配置引用已有 Nginx 安装目录中的该文件。
- `autoindex off`：不展示目录文件列表。
- `location ~ /\.`：拒绝访问隐藏文件路径。

例如：

```text
http://localhost/9Ay6XP/
  → D:/newCode/JavaVueAI/ZeroX/tmp/code_deploy/9Ay6XP/index.html

http://localhost/9Ay6XP/assets/app.js
  → D:/newCode/JavaVueAI/ZeroX/tmp/code_deploy/9Ay6XP/assets/app.js
```

Nginx 直接提供部署文件，不需要把这些请求转发给 Java 后端。后端预览地址
`http://localhost:8123/api/static/...` 仍由 Spring Boot 提供。

## 4. 检查配置并启动

在 PowerShell 中执行：

```powershell
cd D:\newCode\JavaVueAI\ZeroX
.\nginx\manage.ps1 -Action Validate
.\nginx\manage.ps1 -Action Start
.\nginx\manage.ps1 -Action Status
```

管理脚本中的配置检查相当于：

```powershell
& 'D:\develop\nginx-1.22.0-web\nginx.exe' `
  -p 'D:/newCode/JavaVueAI/ZeroX/nginx/' `
  -c conf/nginx.conf -t
```

- `-p` 指定本实例的运行前缀，日志、PID 等相对路径以它为基准。
- `-c` 指定 ZeroX 的配置文件，避免误用安装目录的默认配置。
- `-t` 检查配置语法和引用文件，不会启动服务。
- 启动时省略 `-t`，脚本通过隐藏窗口启动后台进程。

Nginx 语法检查可能创建空 PID 文件，管理脚本会把空文件视为尚未运行。
正常启动后会有 master 和 worker 进程，属于正常现象。

## 5. 验证结果

本次启动后的实测：

| 请求 | 结果 |
| --- | --- |
| `http://localhost/9Ay6XP/` | HTTP 200，内容与部署文件完全一致 |
| `http://127.0.0.1/9Ay6XP/` | HTTP 200 |
| `http://[::1]/9Ay6XP/` | HTTP 200 |
| `http://localhost/no-such-app/` | HTTP 404 |

自己验证：

```powershell
Invoke-WebRequest 'http://localhost/9Ay6XP/' | Select-Object StatusCode
Get-Content .\nginx\logs\access.log -Tail 10
Get-Content .\nginx\logs\error.log -Tail 20
```

`access.log` 记录访问路径与状态码；`error.log` 用于排查启动、文件权限、文件不存在等问题。

## 6. 日常管理

```powershell
# 修改 nginx.conf 后，先检查配置，再平滑重载
.\nginx\manage.ps1 -Action Reload

# 查看本项目 Nginx 是否正在运行
.\nginx\manage.ps1 -Action Status

# 平滑停止：等待正在处理的请求结束
.\nginx\manage.ps1 -Action Stop

# 停止后或电脑重启后，再次启动
.\nginx\manage.ps1 -Action Start
```

本次没有设置开机自启。部署新应用或重新部署已有应用时，只是新增或替换文件，
不需要重载 Nginx；修改 Nginx 配置时才需要重载。

前端开发配置 `VITE_DEPLOY_DOMAIN=http://localhost` 和后端默认
`code.deploy-host=http://localhost` 已与此配置一致，无需重启前后端。
生产前端目前的 `VITE_DEPLOY_DOMAIN=/dist` 不属于本次本地运行方式；
之后部署生产前端时，需要同步设置为实际的部署站点地址。

当前配置按静态文件路径查找。若未来生成的 Vue 应用使用 history 路由，刷新其内部路径
出现 404 时，再增加限定在应用目录内的 SPA 回退规则。资源也需要按子目录部署方式生成。

迁移项目目录时，要同步修改 `nginx.conf` 的 `root`；迁移 Nginx 安装目录时，要修改
`mime.types` 引用和管理脚本中的默认可执行文件位置。
