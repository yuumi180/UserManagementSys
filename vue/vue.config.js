// vue.config.js
module.exports = {
    devServer: {
        port: 8080,
        proxy: {
            '/api': { // 捕获所有以/api开头的请求
                target: 'http://localhost:9090',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': '/api' // 关键修改：将开头的/api保留
                }
            }
        }
    }
}