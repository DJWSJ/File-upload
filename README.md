## 使用deepseek写的springboot文件上传与管理
译使用请使用**gradle JVM-24及其以下版本jdk**进行编译即可
### 下面是api接口使用方式与调用方法
/api/;,根路径重定向,调用方法:GET ~~其实这个没什么用~~<br>
/api/file-manager,前端文件列表获取,调用方法GET<br>
/api/categories/stats,前端文件分类统计,调用方法GET<br>
/api/api/delete/{{fileName}},前端文件删除,调用方法POST<br>
/api/download/{{fileName}},前端文件下载,调用方法GET<br>
/api/upload,前端文件上传,调用方法POST<br>
/api/health,接口健康检查,调用方法GET,~~感觉并不是很重要死了的时候就是死了~~<br>
### 软件端接口文档
/api/v1/files,获取文件列表,调用方法GET<br>
/api/v1/files/download/{{fileName}},文件下载,调用方法GET<br>
/api/v1/files/upload,文件上传,调用方法GET<br>
/api/v1/files/upload/batch,多文件上传,调用方法GET<br>
/api/v1/files/{{fileName}},文件删除接口,调用DELETE<br>
/api/v1/files/{{fileName}}/info,文件大小信息获取,调用GET<br>



## 只是拿来写着玩的大佬不要喷