local wifiConfig = {}
wifiConfig.staticIp = {}

if(file.open("setup.lua", "r") ~= nil) then
	node.compile("setup.lua")
	file.close()
	file.remove("setup.lua")
end

if(file.open("run.lua", "r") ~= nil) then
	node.compile("run.lua")
	file.close()
	file.remove("run.lua")
end

if(file.open("config", "r") ~= nil) then
	dofile("run.lc")
else
	dofile("setup.lc")
end