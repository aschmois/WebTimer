--------------------------------------
-- Upgrader module for NODEMCU
-- LICENCE: http://opensource.org/licenses/MIT
-- cloudzhou<wuyunzhou@espressif.com> - Heavily modified by aschmois
--------------------------------------

--[[
update('file.lua', 'http://IP.ADRESS/path/file.lua')
]]--

local header = ''
local isTruncated = false
local length = nil
local function save(filename, response)
    if isTruncated then
        file.write(response)
        return
    end
    header = header..response
    local i, j = string.find(header, '\r\n\r\n')
    if i == nil or j == nil then
        return
    end
    prefixBody = string.sub(header, j+1, -1)
    file.write(prefixBody)
    header = ''
    isTruncated = true
	--print(string.gsub(response, "\r\n", " rn "))
	--print(response)
	-- Attempt to find the file size
	local x, z = string.find(response, 'Content-Length: ', 1, true)
	if x == nil or z == nil then
		print("No file size reported")
		return
	end
	local u, w = string.find(response, '\r', z)
	if u == nil or w == nil then
		print("Broken HTTP response")
		return
	end
	length = string.sub(response, z+1,u-1)
	if(tonumber(length) ~=nil) then
		length = tonumber(length)
		print("Reported file size: " .. length)
	else
		length = nil
	end
    return
end

----
function update(filename, url, cn, restart)
	local tmpError = nil
	local running = true
	local error = nil
	local success = false
	print("Downloading from: " .. url)
    local ip, port, path = string.gmatch(url, 'http://([0-9.]+):?([0-9]*)(/.*)')()
    if ip == nil then
        return false
    end
    if port == nil or port == '' then
        port = 80
    end
    port = port + 0
    if path == nil or path == '' then
        path = '/'
    end
	print("-- Detailed Connection Info --")
	print("IP: ".. ip)
	print("Port: ".. port)
	print("Path: ".. path)
	print("-- END --")
	file.open(filename, 'w')
	local function timeout() 
		error = tmpError
		file.remove(filename)
		conn:close()
		running = false
	end
	local request = table.concat({"GET ", path,
						" / HTTP/1.1\r\n", 
						"Host: ", ip, "\r\n",
						"Connection: close\r\n",
						"Accept: */*\r\n",
						"User-Agent: Mozilla/4.0 (compatible; esp8266 Lua;)",
						"\r\n\r\n"})
    conn = net.createConnection(net.TCP, false)
	conn:on('connection', function(sck, response)
		tmr.stop(1)
		tmpError = "READ TIMEOUT"
		tmr.alarm(1, 10000, 0, timeout)
		conn:send(request)
	end)
    conn:on('receive', function(sck, response)
		tmr.stop(1)
		tmpError = "READ(2) TIMEOUT"
		tmr.alarm(1, 10000, 0, timeout)
        save(filename, response)
    end)
    conn:on('disconnection', function(sck, response)
		tmr.stop(1)
        local function reset()
			file.flush()
			file.close()
			local list = file.list()
			for k,v in pairs(list) do
				if(filename == k) then
					if(v == 0 or (length ~= nil and v ~= length)) then
						error = "SIZE MISMATCH"
						success = false
					else
						success = true
					end
				end
			end
            header = ''
            isTruncated = false
			length = nil
			if(success) then
				print(filename..' saved')
			else
				print("Could not download `".. filename.."`")
			end
			running = false
        end
        tmr.alarm(0, 2000, 0, reset)
    end)
    conn:connect(port, ip)
	tmpError = "CONN TIMEOUT"
	tmr.alarm(1, 10000, 0, timeout)
	tmr.alarm(2, 1000, 1, function()
		if(running == false) then
			tmr.stop(2)
			local buf = ''
			if(success) then
				buf = buf.."HTTP/1.1 200 OK\r\nServer: WiFi Relay\r\nContent-Type: text/plain\r\n\r\n"
				buf = buf.."1"
			else
				file.remove(filename)
				buf = buf.."HTTP/1.1 500\r\nServer: WiFi Relay\r\nContent-Type: text/plain\r\n\r\n"
				buf = buf.."0"
				buf = buf.."\n"
				if(error ~= nil) then
					buf = buf..error
				else
					buf = buf.."UNKNOWN ERROR"
				end
			end
			cn:send(buf)
			cn:close()
			if(restart == "1") then
				tmr.alarm(3, 2000, 0, function()
					node.restart()
				end)
			end
		end
	end)
	return true
end

