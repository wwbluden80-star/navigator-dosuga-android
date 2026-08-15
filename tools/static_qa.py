from pathlib import Path
import json, re, hashlib, xml.etree.ElementTree as ET, sys
root=Path(__file__).resolve().parents[1]
checks=[]
def ck(name,cond,detail=''):
 checks.append((name,bool(cond),detail)); print(('PASS' if cond else 'FAIL'),name,detail); return bool(cond)
seed=root/'app/src/main/assets/seed'
expected={'mushrooms':61,'fishing':14,'beautiful':102,'cinema':38,'history':23,'events':22}
for ds,n in expected.items():
 o=json.loads((seed/f'{ds}.json').read_text())
 rows=o['items']
 ck(f'seed_{ds}_count',len(rows)==n,f'{len(rows)}/{n}')
 ids=[x['id'] for x in rows]
 ck(f'seed_{ds}_unique',len(ids)==len(set(ids)))
 def valid_coord(x):
  lat=x.get('lat'); lon=x.get('lon')
  if lat is None or lon is None: return lat is None and lon is None
  return -90<=float(lat)<=90 and -180<=float(lon)<=180 and not(float(lat)==0 and float(lon)==0)
 ck(f'seed_{ds}_coords',all(valid_coord(x) for x in rows))
manifest=json.loads((seed/'manifest.json').read_text());ck('manifest_platform',manifest['contentPlatformVersion']=='V16.3')
for ds in expected:
 if ds=='events': continue
 sha=hashlib.sha256((seed/f'{ds}.json').read_bytes()).hexdigest();ck(f'manifest_sha_{ds}',manifest['datasets'][ds]['sha256']==sha)
for xml in root.glob('app/src/main/res/**/*.xml'):
 try: ET.parse(xml); ck('xml_'+xml.relative_to(root).as_posix(),True)
 except Exception as e: ck('xml_'+xml.relative_to(root).as_posix(),False,str(e))
try: ET.parse(root/'app/src/main/AndroidManifest.xml');ck('android_manifest_xml',True)
except Exception as e:ck('android_manifest_xml',False,str(e))
# No web shell technologies in Android source.
source='\n'.join(p.read_text(errors='ignore') for p in root.glob('app/src/main/java/**/*.kt'))
for forbidden in ['android.webkit.WebView','TrustedWebActivity','Capacitor','loadUrl("http']:
 ck('no_'+forbidden.replace('.','_').replace('"',''),forbidden not in source)
# Every explicit R.drawable reference resolves.
refs=set(re.findall(r'(?<!android\.)R\.drawable\.([A-Za-z0-9_]+)',source)); assets={p.stem for p in root.glob('app/src/main/res/drawable*/*')}; missing=sorted(refs-assets);ck('drawable_refs',not missing,','.join(missing))
# Basic Kotlin source lexical guard: remove strings/comments, then balance bracket families.
def scrub(s):
 s=re.sub(r"'(?:\\.|[^'\\])*'","''",s);s=re.sub(r'""".*?"""','',s,flags=re.S);s=re.sub(r'"(?:\\.|[^"\\])*"','""',s);s=re.sub(r'/\*.*?\*/','',s,flags=re.S);s=re.sub(r'//.*','',s);return s
for p in root.glob('app/src/main/java/**/*.kt'):
 s=scrub(p.read_text()); ok=all(s.count(a)==s.count(b) for a,b in [('(',')'),('[',']'),('{','}')]);ck('kotlin_balance_'+p.name,ok)
# Core required architecture strings.
for name,needle in [('compose','androidx.compose'),('room','androidx.room'),('workmanager','androidx.work'),('maplibre','org.maplibre'),('fused_location','FusedLocationProviderClient'),('target36','targetSdk = 36')]:
 hay=(root/'app/build.gradle.kts').read_text()+source;ck('architecture_'+name,needle in hay)
failed=[x for x in checks if not x[1]]
print(f'QA_TOTAL={len(checks)} PASS={len(checks)-len(failed)} FAIL={len(failed)}')
sys.exit(1 if failed else 0)
