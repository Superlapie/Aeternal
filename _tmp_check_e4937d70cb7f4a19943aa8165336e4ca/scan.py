from pathlib import Path
locdat = Path(r'D:\\CodingProjects\\OSRSRSPS\\_tmp_check_e4937d70cb7f4a19943aa8165336e4ca\\donor\.dat').read_bytes()
class R:
    def __init__(self, d, p=0): self.d=d; self.p=p
    def ub(self): b=self.d[self.p]; self.p+=1; return b
    def sb(self): b=self.ub(); return b-256 if b>127 else b
    def us(self): return (self.ub()<<8)|self.ub()
    def sh(self): v=self.us(); return v-65536 if v>32767 else v
    def s(self):
        start=self.p
        while self.d[self.p] != 0:
            self.p += 1
        s=self.d[start:self.p].decode('latin1', errors='replace')
        self.p += 1
        return s
r = R(locdat)
obj_id = -1
hits = []
while r.p < len(locdat):
    obj_id += 1
    name = None
    actions = [None]*5
    try:
        while True:
            op = r.ub()
            if op == 0:
                break
            elif op == 1:
                ln = r.ub()
                for _ in range(ln): r.us(); r.ub()
            elif op == 2:
                name = r.s()
            elif op == 3:
                r.s()
            elif op == 5:
                ln = r.ub();
                for _ in range(ln): r.us()
            elif op in (14,15,17,18,19,21,22,23,27,28,69,75,81):
                if op == 19: r.ub()
                elif op == 28: r.ub()
                elif op == 69: r.ub()
                elif op == 75: r.ub()
                elif op == 81: r.ub()
            elif op in (29,39):
                r.sb()
            elif op in (24,65,66,67,68,82):
                r.us()
            elif 30 <= op < 35:
                actions[op-30] = r.s()
            elif op == 40:
                ln = r.ub();
                for _ in range(ln): r.us(); r.us()
            elif op == 41:
                ln = r.ub();
                for _ in range(ln): r.us(); r.us()
            elif op == 61:
                r.us()
            elif op in (70,71,72):
                r.sh()
            elif op in (73,74,89,90,91):
                pass
            elif op in (77,92):
                r.us(); r.us();
                if op == 92: r.us()
                ln = r.ub()
                for _ in range(ln+1): r.us()
            elif op == 78:
                r.us(); r.ub()
            elif op == 79:
                r.us(); r.us(); r.us(); ln = r.ub();
                for _ in range(ln): r.us()
            elif op == 249:
                ln = r.ub()
                for _ in range(ln):
                    isstr = r.ub()==1
                    r.ub(); r.ub(); r.ub()
                    if isstr: r.s()
                    else: r.us(); r.us()
            else:
                break
        if name in ("Kings' ladder", "Tunnel entrance") or (name and 'ladder' in name.lower()):
            hits.append((obj_id, name, actions))
            print(hits[-1])
            if len(hits) >= 20:
                break
    except Exception:
        break
print('done', len(hits), obj_id, r.p)
