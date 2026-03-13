import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
    baseURL: 'http://localhost:9090',
    timeout: 10000
})

request.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

request.interceptors.response.use(response => {
    let res = response.data;

    if (response.config.responseType === 'blob') {
        return res;
    }

    if (typeof res === 'string') {
        res = res ? JSON.parse(res) : res;
    }
    
    if (res.code === '401') {
        ElMessage.error('未登录或登录已过期');
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        setTimeout(() => {
            window.location.href = '/login';
        }, 1000);
    }
    
    return res;
}, error => {
    console.log('err' + error);
    ElMessage.error(error.message || '请求失败');
    return Promise.reject(error);
});

export default request;