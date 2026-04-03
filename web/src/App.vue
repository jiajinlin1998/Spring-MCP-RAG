<template>
  <div class="app-container">
    <!-- 顶部标题栏 -->
    <div class="header">
      <h1>Spring-AI-智能小助手</h1>
    </div>
    
    <!-- 中间内容区域 -->
    <div class="content">
      <!-- 消息显示区域 -->
      <div class="message-area">
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          :class="['message', msg.type]"
          class="message-wrapper"
        >
          <div class="message-avatar">
            <img v-if="msg.type === 'user'" src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20user%20avatar%20icon%20blue%20circle%20professional&image_size=square" alt="User" />
            <img v-else-if="msg.type === 'bot'" src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=modern%20robot%20avatar%20icon%20purple%20circle%20professional&image_size=square" alt="Bot" />
            <img v-else src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=error%20icon%20red%20circle%20warning&image_size=square" alt="Error" />
          </div>
          <!-- 使用 v-html 渲染 Markdown 转换后的 HTML -->
          <div class="message-content markdown-body" v-html="msg.renderedContent || renderMarkdown(msg.content)"></div>
        </div>
      </div>
    </div>
    
    <!-- 底部操作区域 -->
    <div class="footer">
      <!-- 功能按钮 -->
      <div class="action-buttons">
        <button @click="handleInternetSearch" :class="['action-button', { active: searchMode === 'internet' }]">
          <span class="button-icon">🌐</span>
          联网搜索
        </button>
        <button @click="handleKnowledgeSearch" :class="['action-button', { active: searchMode === 'knowledge' }]">
          <span class="button-icon">📚</span>
          知识搜索
        </button>
      </div>
      
      <!-- 搜索模式提示 -->
      <div v-if="searchMode === 'knowledge'" class="search-mode-indicator">
        <span class="mode-indicator-icon">🔍</span>
        当前模式：知识搜索
      </div>
      <div v-if="searchMode === 'internet'" class="search-mode-indicator">
        <span class="mode-indicator-icon">🌐</span>
        当前模式：联网搜索
      </div>
      
      <!-- 输入和发送区域 -->
      <div class="input-area">
        <input 
          type="text" 
          v-model="message" 
          placeholder="请输入您的问题..."
          class="message-input"
          @keyup.enter="sendMessage"
        />
        <div 
          class="upload-area"
          @click="showUploadModal = true"
        >
          <span class="upload-icon">📁</span>
          上传
        </div>
        <button 
          @click="sendMessage" 
          :disabled="!message.trim() || isLoading"
          class="send-button"
        >
          <span v-if="!isLoading" class="send-icon">🚀</span>
          <span v-else class="loading-icon">⏳</span>
          {{ isLoading ? '发送中...' : '发送' }}
        </button>
      </div>
      
      <!-- 提示信息 -->
      <div class="hint">
        <span class="hint-icon">💡</span>
        更多AI大模型资源v备用：fex1024
      </div>
    </div>
    
    <!-- 上传文档模态框 -->
    <div v-if="showUploadModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>上传文档</h3>
          <button @click="showUploadModal = false" class="close-button">&times;</button>
        </div>
        <div class="modal-body">
          <div 
            class="file-upload-area"
            @dragover.prevent
            @drop.prevent="handleFileDrop"
          >
            <input 
              type="file" 
              ref="fileInput"
              @change="handleFileChange"
              multiple
              accept=".doc,.docx,.txt,.pdf"
              class="file-input"
            />
            <div class="upload-placeholder">
              <div class="upload-icon-large">📁</div>
              <h4>点击或拖拽文件到此处上传</h4>
              <p class="upload-hint">支持 .doc、.docx、.txt、.pdf 格式</p>
            </div>
            <div v-if="selectedFiles.length > 0" class="selected-files">
              <h4>已选择文件：</h4>
              <ul>
                <li v-for="(file, index) in selectedFiles" :key="index" class="file-item">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  <button @click="removeFile(index)" class="remove-file">×</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showUploadModal = false" class="cancel-button">取消</button>
          <button 
            @click="uploadFiles" 
            :disabled="selectedFiles.length === 0 || isUploading"
            class="upload-button"
          >
            <span v-if="!isUploading" class="button-icon">📤</span>
            <span v-else class="loading-icon">⏳</span>
            {{ isUploading ? '上传中...' : '上传' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- 成功提示模态框 -->
    <div v-if="showSuccessModal" class="modal success-modal-container">
      <div class="modal-content success-modal">
        <div class="success-icon-large">✅</div>
        <h3>上传成功</h3>
        <p>{{ successMessage }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { marked } from 'marked';

// 状态管理
const message = ref('');
const messages = ref([]);
const currentUserName = ref('');
const isLoading = ref(false);
const showUploadModal = ref(false);
const selectedFiles = ref([]);
const isUploading = ref(false);
const showSuccessModal = ref(false);
const successMessage = ref('');
const searchMode = ref('default'); // default、knowledge 或 internet
const fileInput = ref(null);
let sse = null;
let currentMessage = '';
let currentBotMsgId = '';

// 生成随机用户名
const generateUserName = () => {
  return 'user_' + Math.random().toString(36).substr(2, 9);
};

// 渲染 Markdown 为 HTML
const renderMarkdown = (content) => {
  if (!content) return '';
  try {
    return marked.parse(content);
  } catch (e) {
    console.error('Markdown 解析错误:', e);
    return content;
  }
};

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 处理文件选择
const handleFileChange = (event) => {
  const files = Array.from(event.target.files);
  selectedFiles.value = [...selectedFiles.value, ...files];
};

// 处理文件拖拽
const handleFileDrop = (event) => {
  const files = Array.from(event.dataTransfer.files);
  selectedFiles.value = [...selectedFiles.value, ...files];
};

// 移除文件
const removeFile = (index) => {
  selectedFiles.value.splice(index, 1);
};

// 上传文件
const uploadFiles = async () => {
  if (selectedFiles.value.length === 0) return;
  
  isUploading.value = true;
  
  try {
    let successCount = 0;
    for (const file of selectedFiles.value) {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await fetch('/rag/upload', {
        method: 'POST',
        body: formData
      });
      
      if (response.ok) {
        successCount++;
      } else {
        messages.value.push({
          type: 'error',
          content: `文件 "${file.name}" 上传失败`,
          renderedContent: null
        });
        scrollToBottom();
      }
    }
    
    // 上传完成后显示成功提示
    if (successCount > 0) {
      successMessage.value = `成功上传 ${successCount} 个文件！`;
      showSuccessModal.value = true;
      // 2秒后自动关闭提示框
      setTimeout(() => {
        showSuccessModal.value = false;
      }, 2000);
    }
    
    // 上传完成后清空选择
    selectedFiles.value = [];
    showUploadModal.value = false;
  } catch (error) {
    console.error('上传错误:', error);
    messages.value.push({
      type: 'error',
      content: '上传失败，请检查网络连接',
      renderedContent: null
    });
    scrollToBottom();
  } finally {
    isUploading.value = false;
  }
};

// 组件挂载时生成用户名并连接 SSE
onMounted(() => {
  currentUserName.value = generateUserName();
  connectSSE();
});

// 连接 SSE
const connectSSE = () => {
  if (sse) {
    sse.close();
  }
  
  console.log('正在建立 SSE 连接...');
  sse = new EventSource(`/SSE/connect?userId=${currentUserName.value}`);
  
  // 处理 ADD 类型消息 - 流式输出
  sse.addEventListener('add', (event) => {
    console.log('收到 ADD 事件:', event.data);
    currentMessage += event.data;
    updateLastBotMessage(currentMessage);
    scrollToBottom();
  });
  
  // 处理 FINISH 类型消息 - 完整响应
  sse.addEventListener('finish', (event) => {
    console.log('收到 FINISH 事件:', event.data);
    try {
      const responseData = JSON.parse(event.data);
      if (responseData.message) {
        // 使用完整的 Markdown 内容替换流式累积的内容
        currentMessage = responseData.message;
        currentBotMsgId = responseData.botMsgId || '';
        updateLastBotMessage(currentMessage, true);
        scrollToBottom();
      }
    } catch (e) {
      console.error('解析 FINISH 消息失败:', e);
    }
    isLoading.value = false;
  });
  
  // 处理默认 message 事件
  sse.addEventListener('message', (event) => {
    console.log('收到默认消息:', event.data);
  });
  
  sse.addEventListener('error', (event) => {
    console.error('SSE 连接错误:', event);
    if (event.readyState === EventSource.CLOSED) {
      console.log('SSE 连接已关闭');
    }
    isLoading.value = false;
  });
  
  sse.addEventListener('open', () => {
    console.log('SSE 连接已建立');
  });
};

// 更新最后一条机器人消息
const updateLastBotMessage = (content, isComplete = false) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'bot') {
    lastMessage.content = content;
    // 如果是完整消息，预渲染 Markdown
    if (isComplete) {
      lastMessage.renderedContent = renderMarkdown(content);
    }
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    const messageArea = document.querySelector('.message-area');
    if (messageArea) {
      messageArea.scrollTop = messageArea.scrollHeight;
    }
  });
};

// 发送消息到后端
const sendMessage = async () => {
  if (!message.value.trim() || isLoading.value) return;
  
  isLoading.value = true;
  
  try {
    // 添加用户问题到消息列表
    messages.value.push({ 
      type: 'user', 
      content: message.value,
      renderedContent: null
    });
    
    // 添加一个空的机器人消息占位
    messages.value.push({ 
      type: 'bot', 
      content: '',
      renderedContent: ''
    });
    
    // 重置消息状态
    currentMessage = '';
    currentBotMsgId = '';
    
    scrollToBottom();
    
    // 构造请求数据
    const chatEntity = {
      currentUserName: currentUserName.value,
      message: message.value,
      botMsgId: currentBotMsgId
    };
    
    // 清空输入框
    message.value = '';
    
    // 根据搜索模式选择不同的接口
    let apiUrl;
    if (searchMode.value === 'knowledge') {
      apiUrl = '/rag/search';
    } else if (searchMode.value === 'internet') {
      apiUrl = '/internet/search';
    } else {
      apiUrl = '/chat/doChat';
    }
    
    // 发送请求
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(chatEntity)
    });
    
    // 发送成功后重置搜索模式为默认
    if (searchMode.value === 'knowledge' || searchMode.value === 'internet') {
      searchMode.value = 'default';
    }
    
    if (!response.ok) {
      console.error('发送失败:', response.status);
      messages.value.pop(); // 移除空的机器人消息
      messages.value.push({ 
        type: 'error', 
        content: '发送失败，请重试',
        renderedContent: null
      });
      isLoading.value = false;
    }
  } catch (error) {
    console.error('发送错误:', error);
    messages.value.pop(); // 移除空的机器人消息
    messages.value.push({ 
      type: 'error', 
      content: '网络错误，请检查连接',
      renderedContent: null
    });
    isLoading.value = false;
  }
};

// 联网搜索按钮点击事件
const handleInternetSearch = () => {
  searchMode.value = 'internet';
};

// 知识搜索按钮点击事件
const handleKnowledgeSearch = () => {
  searchMode.value = 'knowledge';
};

// 组件卸载时关闭 SSE 连接
onUnmounted(() => {
  if (sse) {
    sse.close();
  }
});
</script>

<style scoped>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: #f5f7fa;
}

/* 顶部标题栏 */
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

/* 中间内容区域 */
.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.message-area {
  height: 100%;
  overflow-y: auto;
  padding-right: 10px;
}

/* 滚动条样式 */
.message-area::-webkit-scrollbar {
  width: 6px;
}

.message-area::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 10px;
}

.message-area::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 10px;
}

.message-area::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.message-wrapper {
  margin-bottom: 20px;
  display: flex;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  flex-direction: row-reverse;
}

.message.bot {
  flex-direction: row;
}

.message.error {
  flex-direction: row;
}

.message-avatar {
  width: 40px;
  height: 40px;
  margin: 0 10px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: normal;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.bot .message-content {
  background-color: white;
  border-bottom-left-radius: 4px;
  border: 1px solid #eaeaea;
}

.message.error .message-content {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  border-bottom-left-radius: 4px;
}

/* Markdown 样式 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin-top: 0;
  margin-bottom: 10px;
  font-weight: 600;
}

.markdown-body :deep(p) {
  margin: 0 0 10px 0;
}

.markdown-body :deep(code) {
  background-color: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 10px 0;
}

.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 10px 0;
  padding-left: 20px;
}

.markdown-body :deep(li) {
  margin: 5px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #667eea;
  padding-left: 10px;
  margin: 10px 0;
  color: #666;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.markdown-body :deep(th) {
  background-color: #f5f5f5;
}

/* 底部操作区域 */
.footer {
  padding: 20px;
  border-top: 1px solid #eaeaea;
  background-color: white;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

/* 功能按钮 */
.action-buttons {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid #eaeaea;
  border-radius: 20px;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.action-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-color: #667eea;
}

.action-button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: #667eea;
}

.button-icon {
  font-size: 16px;
}

/* 搜索模式提示 */
.search-mode-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #667eea;
  margin-bottom: 15px;
  font-weight: 600;
  padding: 8px 12px;
  background-color: rgba(102, 126, 234, 0.1);
  border-radius: 16px;
  width: fit-content;
}

.mode-indicator-icon {
  font-size: 14px;
}

/* 输入和发送区域 */
.input-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 25px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.message-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.3);
}

.upload-area {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 12px;
  border: 1px dashed #eaeaea;
  border-radius: 18px;
  background-color: white;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: #667eea;
  color: #667eea;
  background-color: rgba(102, 126, 234, 0.05);
}

.upload-icon {
  font-size: 16px;
}

.send-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.send-icon {
  font-size: 16px;
}

.loading-icon {
  font-size: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 提示信息 */
.hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

.hint-icon {
  font-size: 14px;
}

/* 模态框样式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-in-out;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  animation: slideIn 0.3s ease-in-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #eaeaea;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  transition: color 0.3s ease;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-button:hover {
  color: #333;
  background-color: #f5f5f5;
}

.modal-body {
  padding: 24px;
}

.file-upload-area {
  border: 2px dashed #eaeaea;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  position: relative;
  background-color: #f9f9f9;
  transition: all 0.3s ease;
}

.file-upload-area:hover {
  border-color: #667eea;
  background-color: rgba(102, 126, 234, 0.05);
}

.file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 1;
}

.upload-placeholder {
  position: relative;
  z-index: 0;
}

.upload-icon-large {
  font-size: 64px;
  margin-bottom: 20px;
  color: #667eea;
}

.upload-placeholder h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.upload-placeholder p {
  margin: 0;
  color: #666;
}

.upload-hint {
  font-size: 14px;
  color: #999;
  margin-top: 10px;
}

.selected-files {
  margin-top: 24px;
  text-align: left;
}

.selected-files h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.selected-files ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 10px;
  transition: all 0.3s ease;
}

.file-item:hover {
  background-color: #f0f0f0;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #999;
  margin-right: 10px;
}

.remove-file {
  background: none;
  border: none;
  color: #ff4d4f;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.remove-file:hover {
  background-color: rgba(255, 77, 79, 0.1);
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #eaeaea;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-button {
  padding: 10px 20px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.cancel-button:hover {
  border-color: #667eea;
  color: #667eea;
}

.upload-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.3);
}

.upload-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.upload-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 成功提示模态框样式 */
.success-modal-container {
  background-color: rgba(0, 0, 0, 0.3);
}

.success-modal {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px;
  text-align: center;
  max-width: 400px;
}

.success-icon-large {
  font-size: 64px;
  margin-bottom: 20px;
  color: #52c41a;
}

.success-modal h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.success-modal p {
  margin: 0;
  font-size: 16px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header h1 {
    font-size: 18px;
  }
  
  .content {
    padding: 15px;
  }
  
  .footer {
    padding: 15px;
  }
  
  .message-content {
    max-width: 80%;
  }
  
  .input-area {
    flex-wrap: wrap;
  }
  
  .upload-area {
    order: -1;
    flex: 1;
  }
}
</style>
