#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# DSH 掌上版 · Termux 一键引导脚本（在 Termux 里运行本文件）
# 作用：装 Node.js LTS + DSH 内核，生成 dsh-mobile 启动器
# ============================================================
set -e

echo "==> [1/4] 更新软件源并安装 Node.js (LTS) ..."
pkg update -y
pkg install -y nodejs-lts

echo "==> [2/4] 安装 DSH 内核 (@deepseek-ai/dsh) ..."
npm install -g @deepseek-ai/dsh@0.1.5-rc.2

echo "==> [3/4] 生成启动器 ..."
mkdir -p ~/.dsh-mobile
cat > ~/.dsh-mobile/start.sh <<'EOF'
#!/data/data/com.termux/files/usr/bin/bash
# 启动 dsh web，捕获带 token 的地址，顺手拉起 App
termux-wake-lock 2>/dev/null || true
mkdir -p ~/.dsh-mobile
: > ~/.dsh-mobile/dsh.log
nohup npx @deepseek-ai/dsh web --no-open > ~/.dsh-mobile/dsh.log 2>&1 &
echo "等待 DSH 启动（首次可能要装依赖，耐心等）..."
URL=""
for i in $(seq 1 90); do
  URL=$(grep -m1 -o 'http://127.0.0.1:[0-9]*/?token=[A-Za-z0-9_\-]*' ~/.dsh-mobile/dsh.log 2>/dev/null || true)
  if [ -n "$URL" ]; then break; fi
  sleep 1
done
if [ -z "$URL" ]; then
  echo "启动超时，日志："
  tail -n 20 ~/.dsh-mobile/dsh.log
  exit 1
fi
echo "$URL" > ~/.dsh-mobile/url.txt
echo "DSH 已启动：$URL"
am start -n ai.deepseek.dsh.mobile.debug/ai.deepseek.dsh.mobile.MainActivity -e url "$URL" && echo "已唤起 App"
EOF
chmod +x ~/.dsh-mobile/start.sh
ln -sf ~/.dsh-mobile/start.sh "$PREFIX/bin/dsh-mobile"

echo "==> [4/4] 完成！"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " 以后启动 DSH 只需要在 Termux 里输入："
echo ""
echo "     dsh-mobile"
echo ""
echo " 它会自动启动内核并唤起 App。"
echo " 进阶：装 Termux:Boot 插件可实现开机自启。"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
