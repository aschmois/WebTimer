doCompile("upgrader")
doCompile("utils")
doCompile("factoryReset")
doCompile("setup")
doCompile("run")
collectgarbage()

dofile("utils.lc")
dofile("factoryReset.lc")
dofile("upgrader.lc")
if(file.open("config", "r") ~= nil) then
	dofile("run.lc")
else
	dofile("setup.lc")
end