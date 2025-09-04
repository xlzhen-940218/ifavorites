from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
import time


def get_video_urls(url):
    """
    访问指定的URL，执行JavaScript并返回URL列表。
    """
    options = webdriver.ChromeOptions()
    # 可以选择无头模式，这样浏览器窗口不会显示出来
    options.add_argument("--headless")
    options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")

    # 使用webdriver_manager自动管理和安装chromedriver
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)

    try:
        # 访问URL
        driver.get(url)

        # 等待页面加载，特别是JavaScript内容
        # 实际情况可能需要更智能的等待方式
        time.sleep(5)

        # 定义要执行的JavaScript代码
        js_code = """
        let playlist = document.getElementsByClassName('yt-simple-endpoint style-scope ytd-playlist-panel-video-renderer');
        let urls = [];
        for(var i = 0;i<playlist.length;i++){
            if(playlist[i].id == 'wc-endpoint'){
                var url = playlist[i].href;
                url = url.split("&list")[0];
                urls.push(url);
            }
        }
        return urls;
        """

        # 执行JavaScript并获取返回结果
        urls = driver.execute_script(js_code)

        return urls

    except Exception as e:
        print(f"发生错误: {e}")
        return []
    finally:
        # 确保在任何情况下都关闭浏览器
        driver.quit()


# 示例使用
if __name__ == "__main__":
    target_url = "https://www.youtube.com/watch?v=RGSCd9QGoCc&list=RDRGSCd9QGoCc&start_radio=1"
    video_urls = get_video_urls(target_url)
    if video_urls:
        print("获取到的视频URL列表:")
        for url in video_urls:
            print(url)
    else:
        print("未能获取到视频URL。")