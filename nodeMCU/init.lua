gpio1 = 10
local function doCompile(lua)
	local name = lua..".lua"
	
	if(file.open(name, "r") ~= nil) then
		print("Compiling: " .. lua)
		node.compile(name)
		file.close()
		file.remove(name)
	end
end

doCompile("upgrader")
doCompile("utils")
doCompile("factoryReset")
doCompile("setup")
doCompile("run")
dofile("utils.lc")
dofile("factoryReset.lc")
dofile("upgrader.lc")
if(file.open("config", "r") ~= nil) then
	dofile("run.lc")
else
	dofile("setup.lc")
end
