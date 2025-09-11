const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 1000,
    height: 800
  });

  mainWindow.loadFile('index.html');
  //mainWindow.webContents.openDevTools();

  // 为主窗口的 webContents 设置一个窗口打开处理程序
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    // 在这里你可以根据 url 判断是否需要打开新窗口
    // 并且可以自定义新窗口的参数
    return {
      action: 'allow',
      overrideBrowserWindowOptions: {
        width: 1000, // 设置新窗口的宽度
        height: 800, // 设置新窗口的高度
        // 还可以设置其他 BrowserWindow 选项，例如：
        // show: false,
        // resizable: false,
        // ...
      }
    };
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
