package patroni

def appId = super.binding.getProperty('appId') as int
def instanceIndex = super.binding.getProperty('instanceIndex') as int

def dataDir = super.binding.getProperty('dataDir') as String

"""
[app${appId}]
pg${instanceIndex}-path=${dataDir}

[global]
repo${appId}-path=/var/lib/pgbackrest

[global:archive-push]
compress-level=3
"""