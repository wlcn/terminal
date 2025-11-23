// WebSocket测试脚本 - 用于测试终端应用的正确性
const WebSocket = require('ws');

// 配置
const WS_URL = 'ws://localhost:8080/ws/terminal';
const TEST_SESSION_ID = 'test-session-' + Date.now();

// 创建WebSocket连接
console.log('正在连接到WebSocket服务器:', WS_URL);
const ws = new WebSocket(WS_URL);

// 消息计数器
let messageCount = 0;
let outputCount = 0;

// 连接建立事件
ws.on('open', function open() {
    console.log('✅ WebSocket连接已建立');
    
    // 发送会话创建消息
    const createSessionMsg = {
        type: 'CREATE_SESSION',
        sessionId: TEST_SESSION_ID,
        command: '/bin/bash',
        args: ['-i'],
        workingDirectory: '/tmp',
        env: {}
    };
    
    console.log('📤 发送会话创建消息:', JSON.stringify(createSessionMsg));
    ws.send(JSON.stringify(createSessionMsg));
});

// 接收消息事件
ws.on('message', function message(data) {
    messageCount++;
    const message = data.toString();
    
    try {
        const parsed = JSON.parse(message);
        
        if (parsed.type === 'SESSION_CREATED') {
            console.log('✅ 会话创建成功:', parsed.sessionId);
            
            // 等待1秒后发送测试命令
            setTimeout(() => {
                const testCommands = [
                    'echo "Hello Terminal!"',
                    'pwd',
                    'ls -la',
                    'whoami',
                    'date'
                ];
                
                console.log('📤 开始发送测试命令...');
                
                // 发送测试命令
                testCommands.forEach((cmd, index) => {
                    setTimeout(() => {
                        const inputMsg = {
                            type: 'TERMINAL_INPUT',
                            sessionId: TEST_SESSION_ID,
                            input: cmd + '\n'
                        };
                        
                        console.log(`📤 发送命令 ${index + 1}: ${cmd}`);
                        ws.send(JSON.stringify(inputMsg));
                    }, index * 2000); // 每2秒发送一个命令
                });
                
                // 10秒后关闭会话
                setTimeout(() => {
                    const closeMsg = {
                        type: 'CLOSE_SESSION',
                        sessionId: TEST_SESSION_ID
                    };
                    
                    console.log('📤 发送关闭会话消息');
                    ws.send(JSON.stringify(closeMsg));
                    
                    // 3秒后关闭连接
                    setTimeout(() => {
                        console.log('🔌 关闭WebSocket连接');
                        ws.close();
                    }, 3000);
                }, testCommands.length * 2000 + 3000);
            }, 1000);
        }
        else if (parsed.type === 'TERMINAL_OUTPUT') {
            outputCount++;
            const output = parsed.output.replace(/\n/g, '\\n').replace(/\r/g, '\\r');
            console.log(`📥 收到终端输出 [${outputCount}]: '${output}'`);
        }
        else if (parsed.type === 'SESSION_TERMINATED') {
            console.log('🔴 会话已终止:', parsed.sessionId);
        }
        else if (parsed.type === 'ERROR') {
            console.log('❌ 错误消息:', parsed.message);
        }
        else {
            console.log('📥 其他消息类型:', parsed.type);
        }
    } catch (error) {
        console.log('📥 原始消息:', message);
    }
});

// 连接关闭事件
ws.on('close', function close() {
    console.log('🔌 WebSocket连接已关闭');
    console.log(`📊 统计信息: 总共收到 ${messageCount} 条消息，其中 ${outputCount} 条终端输出`);
});

// 错误事件
ws.on('error', function error(err) {
    console.error('❌ WebSocket连接错误:', err.message);
});

console.log('🚀 WebSocket测试脚本已启动，等待连接...');