# Screen Stream over HTTP
This is a fork from [dkrivoruchko/ScreenStream](https://github.com/dkrivoruchko/ScreenStream/) 
where the maximum fps limit is increased to 60 instead of 30.

It turns out to be working exceptionally good, with <= 10 ms latency streaming locally from my 
phone connecting to a 5Ghz ac network (1080p screen, 50% scale). Higher scale yields higher 
latency but still it is super low (Around 20 ~ 30 ms for a full 1080p). The great thing about 
it is that you don't have to worry about audio sync issues for even the most action intense 
video games if you have the phone's audio output captured and playing somewhere else simultaneously
with the video.

This change is not for everyone. It requires a high speed and reliable local network (like 5Ghz ac wifi). 
The throughput could also go up to 50Mb/s or more. This is for people who could trade network throughput 
for minimum latency.

Changes are made to the master branch.