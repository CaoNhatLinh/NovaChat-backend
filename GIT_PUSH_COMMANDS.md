# Git Commands để Push Code lên GitHub

## Các bước chuẩn bị và push code:

### 1. Khởi tạo Git repository (nếu chưa có)
```powershell
cd e:\WORKSPACE\ChatApp\chat-service
git init
```

### 2. Kiểm tra trạng thái git
```powershell
git status
```

### 3. Add tất cả các file (trừ những file đã ignore)
```powershell
git add .
```

### 4. Commit các thay đổi
```powershell
git commit -m "Initial commit: NovaChat Backend with real-time messaging, notifications, and search features"
```

### 5. Đổi tên branch chính thành main (nếu đang dùng master)
```powershell
git branch -M main
```

### 6. Thêm remote repository
```powershell
git remote add origin https://github.com/CaoNhatLinh/NovaChat-backend.git
```

### 7. Push code lên GitHub
```powershell
git push -u origin main
```

---

## Nếu repository đã tồn tại trên GitHub và bạn muốn force push:

```powershell
# Sử dụng cẩn thận - chỉ dùng nếu bạn chắc chắn muốn overwrite remote
git push -u origin main --force
```

---

## Kiểm tra remote đã được thêm chưa:
```powershell
git remote -v
```

## Nếu đã có remote và muốn đổi URL:
```powershell
git remote set-url origin https://github.com/CaoNhatLinh/NovaChat-backend.git
```

---

## Các file đã được dọn dẹp:
✅ Đã xóa: hs_err_pid*.log, replay_pid*.log
✅ Đã xóa: thư mục target/
✅ Đã xóa: HELP.md (file generated mặc định)
✅ Đã di chuyển tất cả documentation files vào thư mục docs/
✅ Đã cập nhật .gitignore để ignore log files và sensitive files
✅ Đã tạo README.md chi tiết cho project

---

## Note quan trọng:
- File `serviceAccountKey.json` đã được thêm vào .gitignore để bảo mật
- Nếu file này đã được commit trước đó, bạn cần xóa nó khỏi git history:
  ```powershell
  git rm --cached serviceAccountKey.json
  git commit -m "Remove sensitive file from git"
  ```
