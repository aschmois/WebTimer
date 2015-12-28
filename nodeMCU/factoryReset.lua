local seconds = 0
local function flashQuickly()
	local function checkFactoryResetOff()
		if(gpio.read(3) == 1) then
			node.restart()
		end
	end
	tmr.stop(4)
	tmr.stop(5)
	tmr.stop(6)
	gpio.mode(gpio1, gpio.OUTPUT)
	gpio.write(gpio1, gpio.LOW)
	tmr.alarm(4, 200, 1, toggleGPIO1LED)
	tmr.alarm(3, 1000, 1, checkFactoryResetOff)
end

local function checkFactoryReset()
	if(gpio.read(3) == 0) then
		if(file.open("config", "r") ~= nil) then
			if(seconds == 0) then
				gpio.mode(gpio1, gpio.OUTPUT)
				gpio.write(gpio1, gpio.LOW)
				tmr.alarm(4, 2500, 1, toggleGPIO1LED)
				print("Starting Factory Reset Check")
			end
			seconds = seconds + 1
			if(seconds == 10) then
				print("Factory Reset!")
				file.remove("config")
				node.restart()
			end
		else
			flashQuickly()
		end
	else
		seconds = 0
	end
		
end
tmr.alarm(6, 1000, 1, checkFactoryReset)

-- If at the time of loading, gpio0 is low, it means a factory reset is in order
if(gpio.read(3) == 0) then
	if(file.open("config", "r") ~= nil) then
		print("Factory Reset!")
		file.remove("config")
	else
		flashQuickly()
	end
	file.close("config")
end