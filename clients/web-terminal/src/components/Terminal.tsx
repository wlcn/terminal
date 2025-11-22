import React, { useEffect, useRef } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { WebglAddon } from '@xterm/addon-webgl';
import '@xterm/xterm/css/xterm.css';

interface TerminalComponentProps {
  className?: string;
}

export const TerminalComponent: React.FC<TerminalComponentProps> = ({ className }) => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);

  useEffect(() => {
    if (!terminalRef.current) return;

    // 创建终端实例
    terminal.current = new Terminal({
      theme: {
        background: '#0f172a',
        foreground: '#f8fafc',
        cursor: '#f8fafc',
        selection: '#334155',
      },
      fontSize: 14,
      fontFamily: '"Fira Code", "Cascadia Code", "Courier New", monospace',
      cursorBlink: true,
    });

    // 创建插件
    fitAddon.current = new FitAddon();
    const webLinksAddon = new WebLinksAddon();
    const webglAddon = new WebglAddon();

    // 加载插件
    terminal.current.loadAddon(fitAddon.current);
    terminal.current.loadAddon(webLinksAddon);
    terminal.current.loadAddon(webglAddon);

    // 打开终端
    terminal.current.open(terminalRef.current);
    fitAddon.current.fit();

    // 添加欢迎信息
    terminal.current.writeln('🚀 欢迎使用 Web Terminal');
    terminal.current.writeln('📡 正在连接后端服务...');
    terminal.current.writeln('');

    // 模拟连接过程
    setTimeout(() => {
      terminal.current?.writeln('✅ 后端服务连接成功');
      terminal.current?.writeln('💻 终端已就绪');
      terminal.current?.writeln('');
      terminal.current?.write('$ ');
    }, 1000);

    // 处理窗口大小变化
    const handleResize = () => {
      fitAddon.current?.fit();
    };

    window.addEventListener('resize', handleResize);

    // 处理键盘输入
    terminal.current.onData((data) => {
      // 这里可以添加实际的命令处理逻辑
      terminal.current?.write(data);
    });

    return () => {
      window.removeEventListener('resize', handleResize);
      terminal.current?.dispose();
    };
  }, []);

  return (
    <div className={className}>
      <div 
        ref={terminalRef} 
        className="w-full h-full bg-slate-900 rounded-lg overflow-hidden"
      />
    </div>
  );
};