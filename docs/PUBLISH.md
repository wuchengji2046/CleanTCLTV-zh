# 上传 GitHub

建议仓库名：`CleanTCLTV-zh`。

在 GitHub 创建自己的空仓库，不初始化 README、许可证或 .gitignore。在解压后的源码目录运行：

```bash
git init -b main
git add .
git status --short
git commit -m "Initial TCL ordinary-app home adaptation"
git remote add origin https://github.com/YOUR_USERNAME/CleanTCLTV-zh.git
git push -u origin main
```

替换 YOUR_USERNAME；本机 Git 需先设置提交姓名和邮箱。也可通过 GitHub 网页 Upload files 上传解压后的内容，命令行更适合后续维护。

这是独立源码快照，不包含原仓库 .git 历史。上传前检查暂存清单，保留 MIT 和字体许可证，不要上传电视地址、设备完整诊断、ADB 私钥、签名文件、第三方 APK 或本机 SDK 缓存。`.gitignore` 已排除常见敏感文件与产物。

上述步骤适用于将源码发布到自己的新仓库。只上传本项目目录内容，无需把本次适配过程中的其他输出文件一起上传。
