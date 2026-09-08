"""Run against tools/smoke-server.gradle's disposable in-memory server only."""
import json
from urllib.request import Request, urlopen
from urllib.error import HTTPError

BASE='http://localhost:18080'
def call(path, method='GET', data=None, token=None, expected=200):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    try:
        with urlopen(Request(BASE+path,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method),timeout=10) as response:
            body=response.read();status=response.status
    except HTTPError as error:
        body=error.read();status=error.code
    assert status in (expected if isinstance(expected,tuple) else (expected,)),(method,path,status,body.decode())
    return json.loads(body) if body else None

def account(name):
    credentials={'email':name+'@test.local','password':'SmokeTest123!','nickname':name}
    data=call('/api/auth/signup','POST',credentials,expected=(201,409))
    if 'accessToken' not in data: data=call('/api/auth/login','POST',credentials)
    token=data['accessToken']
    artists=call('/api/artists',token=token)
    genres=call('/api/genres',token=token)
    call('/api/me/taste','PUT',{'artistIds':[a['id'] for a in artists[:3]],'genreIds':[genres[0]['id']]},token)
    call('/api/me/location','PUT',{'lat':36.1034,'lon':129.3897},token)
    return token,data['user']['id']

alice,aid=account('qa_alice')
bob,bid=account('qa_bob')
track={'source':'ITUNES','externalId':'smoke-1','title':'오늘의 산책','artistName':'SoundMap 테스트','albumName':'함께 발견한 음악','albumArtUrl':None,'previewUrl':None}
saved=call('/api/me/now-playing',token=alice,expected=(200,204)) or call('/api/me/now-playing','PUT',track,alice)
if not call('/api/me/now-playing',token=bob,expected=(200,204)):call('/api/me/now-playing','PUT',track,bob)
tid=saved['track']['id']
call('/api/me/saved-tracks/'+str(tid),'PUT',token=alice)
call('/api/me/saved-tracks/'+str(tid),'PUT',token=alice)
assert len(call('/api/me/saved-tracks',token=alice))==1
assert call('/api/me/saved-tracks',token=bob)==[]
assert len(call('/api/me/music-history',token=alice)['entries'])==1
assert call('/api/music/local-chart',token=alice)[0]['people']==2
room=call('/api/chats/requests','POST',{'targetUserId':bid},alice,201)['roomId']
call(f'/api/chats/{room}/tracks','POST',track,alice,409)
call(f'/api/chats/{room}/accept','POST',token=bob)
message=call(f'/api/chats/{room}/tracks','POST',track,alice)
assert message['track']['id']==tid
assert call(f'/api/chats/{room}/messages',token=bob)[0]['track']['id']==tid
assert call('/api/me/notifications',token=bob)['unreadCount']==2
call('/api/me/notifications/read-all','POST',token=bob)
assert call('/api/me/notifications',token=bob)['unreadCount']==0
for endpoint in ['/api/me/saved-tracks','/api/me/music-history','/api/music/local-chart','/api/me/notifications']:
    call(endpoint,expected=401)
assert call('/api/me/notifications/push-config',token=alice)['enabled'] is False
call('/api/me/music-history?month=invalid',token=alice,expected=400)
call(f'/api/chats/{room}/tracks','POST',{'title':''},alice,400)
print('API smoke passed: saved tracks, history, chart, music cards, notifications, auth and validation. Room:',room)
