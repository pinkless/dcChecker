// PDP.cpp : 此文件包含 "main" 函数。程序执行将在此处开始并结束。
//

#include <iostream>
#include <openssl/rand.h>
#include <time.h>
using namespace std;
int main()
{
    cout << "First openssl code!" << endl;
    time_t t = time(0);
    unsigned char buf[16] = { 0 };
    int re = RAND_bytes(buf, sizeof(buf));
    for (int i = 0; i < sizeof(buf); i++)
    {
        cout << "[" << (int)buf[i] << "]";
    }
    getchar();
    return 0;
}

