import  json

f = open("media.txt", 'r')
data=f.read()



jsonobj = json.loads(data)
for i in jsonobj:
        print i['name']
