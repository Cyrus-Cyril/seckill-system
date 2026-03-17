document.getElementById("pingBtn").addEventListener("click", async () => {
    const result = document.getElementById("result");
    try {
        const resp = await fetch("/api/test/ping");
        const data = await resp.json();
        result.textContent = JSON.stringify(data, null, 2);
    } catch (e) {
        result.textContent = "请求失败：" + e;
    }
});