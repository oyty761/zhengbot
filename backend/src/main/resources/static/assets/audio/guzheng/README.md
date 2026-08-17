# 古筝 21 弦单音 MP3（D 调）

本目录可直接用于古筝模型的“触弦播放”功能，共 21 个立体声 MP3（44.1 kHz、192 kbps）。

## 弦号约定

- `string_01_D6.mp3`：1 号弦，最高音、最短弦
- `string_21_D2.mp3`：21 号弦，最低音、最长弦
- 升号在文件名中写作 `s`，例如 `Fs4` 表示 F♯4
- 完整映射、频率和时长见 `strings.json`
- 面向人工查看的完整弦号对照表见 `弦号与音频对应说明.md`

标准音高（由低到高）为：

`D2 E2 F#2 A2 B2 | D3 E3 F#3 A3 B3 | D4 E4 F#4 A4 B4 | D5 E5 F#5 A5 B5 | D6`

## 音频处理说明

这批文件来自 Zachary King（Atlas）录制的 Rosewood Guzheng 21 切片音源。源包本身就是 21 根弦各自独立录制的 WAV：每个 MP3 对应一个不同的原始 WAV，不再从连续演奏中自动切段，也没有拿少数音高变调补齐。

转换时采用作者 Ableton 工程内保存的起止点和校音值，只增加极短淡入、尾部淡出及峰值统一处理。21 个文件已逐一做波形与起音检查：每段只有开头一次触弦，随后为自然衰减；音高实测最大偏差为 19.6 cents。

## 网页中使用

```js
const sound = new Audio('/audio/guzheng_21_strings/string_01_D6.mp3');
sound.currentTime = 0;
sound.play();
```

批量加载时可以读取 `strings.json`，用其中的 `stringNumber` 对应模型弦号。

## 来源与许可

- 原作者：Zachary King / Atlas
- 音源：Rosewood Guzheng Tuned Project（21 slices）
- 作者发布帖：https://www.reddit.com/r/ableton/comments/g7een5/
- 作者下载页存档：https://web.archive.org/web/20201021044656/https://zacharykingmusic.com/downloads/
- 作者许可说明：royalty free（免版税）

作者下载页说明该古筝音色可 royalty free 使用；本项目按用户要求用于非商业建模。原始作者压缩包保存在 `source/rosewood_guzheng_21_slice_source.zip`，便于回溯。该发布没有附带标准化的 CC/MIT 许可证文本，因此如果今后改作商业发行，建议再向作者确认具体授权范围。

