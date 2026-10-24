"""Build cuboid UV nets for the back model; never project a whole illustration."""
from pathlib import Path
from PIL import Image, ImageDraw
import re
PARTS = [
 ('backplate',2,2,(-3,-7,.1,6,7,1),'frame'),
 ('left_tank',23,2,(-5,-8,.7,3,8,3),'tank'),
 ('right_tank',39,2,(2,-8,.7,3,8,3),'tank'),
 ('top_bridge',55,2,(-2,-8,.4,4,2,3),'frame'),
 ('crown_cap',74,2,(-3,-9,.7,6,1,3),'plate'),
 ('left_frame_rail',95,2,(-5,-8,.2,1,8,2),'frame'),
 ('right_frame_rail',106,2,(4,-8,.2,1,8,2),'frame'),
 ('power_housing',117,2,(-2,-6,1.2,4,5,1),'plate'),
 ('power_spine',130,2,(-.5,-5,2.2,1,4,1),'light'),
 ('left_bracket',135,2,(-5,-1,.7,1,3,2),'frame'),
 ('right_bracket',2,21,(4,-1,.7,1,3,2),'frame'),
 ('left_thruster',12,21,(-5,0,.7,3,3,3),'plate'),
 ('right_thruster',26,21,(2,0,.7,3,3,3),'plate'),
 ('left_nozzle_collar',40,21,(-5,2,.7,3,1,3),'red'),
 ('right_nozzle_collar',56,21,(2,2,.7,3,1,3),'red'),
 ('left_exhaust',72,21,(-4.5,3,1.2,2,1,2),'frame'),
 ('right_exhaust',84,21,(2.5,3,1.2,2,1,2),'frame')]
PALETTES={'wood':(184,126,67),'iron':(196,207,213),'gold':(235,187,54),'emerald':(44,188,110),'diamond':(64,206,222),'netherite':(112,99,120)}
def shade(c,k): return tuple(max(0,min(255,round(v*k))) for v in c)+(255,)
def build():
 out=Path('src/main/resources/assets/jetpack/textures/entity/back')
 for tier,base in PALETTES.items():
  im=Image.new('RGBA',(150,170),shade(base,.5)); draw=ImageDraw.Draw(im)
  for name,u,v,box,kind in PARTS:
   _,_,_,w,h,d=box; w,h,d=map(int,(w,h,d))
   color=(85,77,73) if kind=='frame' else (192,38,40) if kind in ('red','light') else base
   # Standard Minecraft box net: two caps plus four side faces.
   faces=[(u+d,v,w,d),(u+d+w,v,w,d),(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),(u+2*d+w,v+d,w,h)]
   for idx,(x,y,fw,fh) in enumerate(faces):
    draw.rectangle((x,y,x+fw-1,y+fh-1),fill=shade(color,.8 if idx in (2,4) else 1))
    draw.line((x,y,x+fw-1,y),fill=shade(color,1.3))
    draw.line((x,y+fh-1,x+fw-1,y+fh-1),fill=shade(color,.5))
    if fh>=5:
     draw.line((x,y+1,x,y+fh-2),fill=shade(color,.6))
     draw.point((x+fw-1,y+1),fill=shade(color,1.45))
     if kind=='tank' and fw>=3:
      draw.line((x+fw//2,y+2,x+fw//2,y+fh-3),fill=(255,64,49,255))
     if tier=='wood' and kind not in ('frame','red','light'):
      for yy in range(y+3,y+fh-1,3): draw.line((x+1,yy,x+fw-1,yy),fill=shade(color,.78))
  im.save(out/f'jetpack_{tier}.png')
 if __name__=='__main__':
  p=Path('src/main/java/br/com/petrickfaria/jetpack/client/JetpackBackModel.java'); s=p.read_text()
  start=s.index('        // Every cuboid'); end=s.index('        return LayerDefinition',start)
  block='        // Compact twin tanks, layered core and stepped nozzle assemblies.\n'
  for name,u,v,box,kind in PARTS:
   nums=', '.join(f'{float(n):.2f}F' for n in box)
   block+=f'        pack.addOrReplaceChild("{name}", CubeListBuilder.create().texOffs({u}, {v})\n                .addBox({nums}), PartPose.ZERO);\n'
  p.write_text(s[:start]+block+s[end:])
if __name__=='__main__': build()
