gpio1 = 10
function doCompile(lua)
	local name = lua..".lua"
	
	if(file.open(name, "r") ~= nil) then
		file.close()
		print("Compiling: " .. lua)
		node.compile(name)
		file.remove(name)
	end
end
if(file.open("ota.lua", "r") ~= nil or file.open("ota.lc", "r") ~= nil) then
	file.close()
	if(file.open("ota.lua", "r") ~= nil) then
		file.close()
		print("Doing OTA update...")
		node.compile("ota.lua")
		file.remove("ota.lua")
	else
		print("Continuing OTA update...")
	end
	collectgarbage()
	dofile("ota.lc")
else
	doCompile("normalinit")
	dofile("normalinit.lc")
end